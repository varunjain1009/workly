package com.workly.modules.tracking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TrackingWebSocketHandlerTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private WebSocketSession session;

    private TrackingWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new TrackingWebSocketHandler(redisTemplate);
    }

    private void mockSessionUri(String query) throws Exception {
        URI uri = new URI("ws://localhost/ws/tracking?" + query);
        when(session.getUri()).thenReturn(uri);
        when(session.getId()).thenReturn("sess1");
    }

    @Test
    void afterConnectionEstablished_validParams_registerSession() throws Exception {
        mockSessionUri("jobId=job1&role=SEEKER");
        when(session.isOpen()).thenReturn(true);

        handler.afterConnectionEstablished(session);

        assertNotNull(handler.getLocalSeekerSession("job1"));
        assertEquals(session, handler.getLocalSeekerSession("job1"));
    }

    @Test
    void afterConnectionEstablished_missingParams_closesSession() throws Exception {
        mockSessionUri("jobId=job1");

        handler.afterConnectionEstablished(session);

        verify(session).close(any(CloseStatus.class));
    }

    @Test
    void handleTextMessage_providerRole_publishesToRedis() throws Exception {
        mockSessionUri("jobId=job1&role=PROVIDER");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"lat\":12.9,\"lon\":77.6}"));

        verify(redisTemplate).convertAndSend(eq("tracking:job1"), anyString());
    }

    @Test
    void handleTextMessage_seekerRole_doesNotPublish() throws Exception {
        mockSessionUri("jobId=job1&role=SEEKER");
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"lat\":12.9}"));

        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    void afterConnectionClosed_removesSession() throws Exception {
        mockSessionUri("jobId=job1&role=SEEKER");
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        assertNotNull(handler.getLocalSeekerSession("job1"));

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertNull(handler.getLocalSeekerSession("job1"));
    }

    @Test
    void getLocalSeekerSession_noJob_returnsNull() {
        assertNull(handler.getLocalSeekerSession("nonexistent"));
    }
}
