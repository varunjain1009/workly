package com.workly.modules.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    // jobId -> role -> session (local to this JVM instance)
    // e.g., "job123" -> {"PROVIDER": session1, "SEEKER": session2}
    private final Map<String, Map<String, WebSocketSession>> jobSessions = new ConcurrentHashMap<>();

    static final String TRACKING_CHANNEL_PREFIX = "tracking:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> queryParams = getQueryParams(session);
        String jobId = queryParams.get("jobId");
        String role = queryParams.get("role"); // "PROVIDER" or "SEEKER"

        if (jobId == null || role == null) {
            log.warn("TrackingWebSocketHandler: Connection rejected, missing jobId or role.");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        jobSessions.computeIfAbsent(jobId, k -> new ConcurrentHashMap<>()).put(role.toUpperCase(), session);
        log.info("TrackingWebSocketHandler: Connected Job={} Role={} Session={}", jobId, role.toUpperCase(), session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, String> queryParams = getQueryParams(session);
        String jobId = queryParams.get("jobId");
        String role = queryParams.get("role");

        if (jobId == null || role == null) return;

        // PROVIDER location updates are fanned out via Redis Pub/Sub so every
        // server instance (including this one) delivers to its local SEEKER session.
        if ("PROVIDER".equalsIgnoreCase(role)) {
            redisTemplate.convertAndSend(TRACKING_CHANNEL_PREFIX + jobId, message.getPayload());
            log.debug("TrackingWebSocketHandler: Published location update for Job={} to Redis", jobId);
        }
    }

    /**
     * Returns the local SEEKER WebSocketSession for a job if it is connected to
     * this JVM instance, or {@code null} otherwise. Used by {@link TrackingRedisSubscriber}.
     */
    public WebSocketSession getLocalSeekerSession(String jobId) {
        Map<String, WebSocketSession> participants = jobSessions.get(jobId);
        if (participants == null) return null;
        WebSocketSession seekerSession = participants.get("SEEKER");
        return (seekerSession != null && seekerSession.isOpen()) ? seekerSession : null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, String> queryParams = getQueryParams(session);
        String jobId = queryParams.get("jobId");
        String role = queryParams.get("role");

        if (jobId != null && role != null) {
            Map<String, WebSocketSession> participants = jobSessions.get(jobId);
            if (participants != null) {
                participants.remove(role.toUpperCase());
                if (participants.isEmpty()) {
                    jobSessions.remove(jobId);
                }
            }
            log.info("TrackingWebSocketHandler: Disconnected Job={} Role={} Session={}", jobId, role.toUpperCase(), session.getId());
        }
    }

    private Map<String, String> getQueryParams(WebSocketSession session) {
        String query = session.getUri().getQuery();
        Map<String, String> params = new ConcurrentHashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }
        }
        return params;
    }
}
