package com.workly.modules.job.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OutboxRelaySchedulerTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private OutboxRelayScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        scheduler = new OutboxRelayScheduler(outboxEventRepository, kafkaTemplate, redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void relayEvents_lockNotAcquired_skips() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

        scheduler.relayEvents();

        verify(outboxEventRepository, never()).findTop100ByProcessedFalseOrderByCreatedAtAsc();
    }

    @Test
    void relayEvents_noPendingEvents_doesNothing() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of());
        when(redisTemplate.delete(anyString())).thenReturn(true);

        scheduler.relayEvents();

        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(redisTemplate).delete("outbox:relay:lock");
    }

    @Test
    void relayEvents_sendsEventsAndMarksProcessed() throws Exception {
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        OutboxEvent event = new OutboxEvent();
        event.setId("e1");
        event.setTopic("job.created");
        event.setPayload("payload");
        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc()).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(outboxEventRepository.save(any())).thenReturn(event);
        when(redisTemplate.delete(anyString())).thenReturn(true);

        scheduler.relayEvents();

        verify(kafkaTemplate).send("job.created", "payload");
        verify(outboxEventRepository).save(argThat(e -> e.isProcessed()));
        verify(redisTemplate).delete("outbox:relay:lock");
    }

    @Test
    void relayEvents_kafkaFailure_stopsAndReleasesLock() throws Exception {
        when(valueOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        OutboxEvent event1 = new OutboxEvent();
        event1.setId("e1");
        event1.setTopic("job.created");
        OutboxEvent event2 = new OutboxEvent();
        event2.setId("e2");
        event2.setTopic("job.created");

        when(outboxEventRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event1, event2));
        when(kafkaTemplate.send(anyString(), any()))
                .thenThrow(new RuntimeException("Kafka down"));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        scheduler.relayEvents();

        verify(kafkaTemplate, times(1)).send(anyString(), any()); // stopped after first failure
        verify(redisTemplate).delete("outbox:relay:lock"); // lock always released
    }
}
