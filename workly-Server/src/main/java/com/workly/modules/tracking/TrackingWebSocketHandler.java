package com.workly.modules.tracking;

import lombok.extern.slf4j.Slf4j;
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
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    // jobId -> role -> session
    // e.g., "job123" -> {"PROVIDER": session1, "SEEKER": session2}
    private final Map<String, Map<String, WebSocketSession>> jobSessions = new ConcurrentHashMap<>();

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

        // If the message is from PROVIDER, route to SEEKER
        if ("PROVIDER".equalsIgnoreCase(role)) {
            Map<String, WebSocketSession> participants = jobSessions.get(jobId);
            if (participants != null) {
                WebSocketSession seekerSession = participants.get("SEEKER");
                if (seekerSession != null && seekerSession.isOpen()) {
                    try {
                        seekerSession.sendMessage(new TextMessage(message.getPayload()));
                        log.debug("TrackingWebSocketHandler: Routed location update for Job={} to Seeker", jobId);
                    } catch (IOException e) {
                        log.error("Failed to send tracking payload to seeker", e);
                    }
                }
            }
        }
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
