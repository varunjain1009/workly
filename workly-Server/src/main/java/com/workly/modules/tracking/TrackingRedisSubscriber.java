package com.workly.modules.tracking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * Subscribes to Redis Pub/Sub channels named {@code tracking:{jobId}}.
 * When a PROVIDER publishes a location update on any server instance, Redis
 * fans it out to every instance; this listener then forwards it to the SEEKER
 * WebSocket session that happens to be connected locally.
 *
 * This decouples WebSocket routing from in-process state, so the tracking path
 * works correctly behind a load balancer with multiple JVM instances.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingRedisSubscriber implements MessageListener {

    private final TrackingWebSocketHandler trackingWebSocketHandler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String payload = new String(message.getBody());

        if (!channel.startsWith(TrackingWebSocketHandler.TRACKING_CHANNEL_PREFIX)) {
            return;
        }

        String jobId = channel.substring(TrackingWebSocketHandler.TRACKING_CHANNEL_PREFIX.length());
        WebSocketSession seekerSession = trackingWebSocketHandler.getLocalSeekerSession(jobId);

        if (seekerSession != null) {
            try {
                seekerSession.sendMessage(new TextMessage(payload));
                log.debug("TrackingRedisSubscriber: Delivered location update for Job={} to local Seeker", jobId);
            } catch (IOException e) {
                log.error("TrackingRedisSubscriber: Failed to forward tracking update for Job={}", jobId, e);
            }
        }
    }
}
