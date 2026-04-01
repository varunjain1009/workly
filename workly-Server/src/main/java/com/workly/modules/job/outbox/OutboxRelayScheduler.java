package com.workly.modules.job.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    // Distributed lock: only one instance may run the relay at a time.
    // TTL is generous enough to cover a full relay batch but short enough to
    // recover quickly if the lock holder crashes without releasing.
    private static final String LOCK_KEY = "outbox:relay:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    @Scheduled(fixedDelay = 5000)
    public void relayEvents() {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return; // Another instance is already processing
        }

        try {
            List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
            if (pendingEvents.isEmpty()) return;

            log.debug("Found {} pending outbox events to process", pendingEvents.size());

            for (OutboxEvent event : pendingEvents) {
                try {
                    kafkaTemplate.send(event.getTopic(), event.getPayload()).get();
                    event.setProcessed(true);
                    outboxEventRepository.save(event);
                    log.debug("Successfully relayed outbox event {}", event.getId());
                } catch (Exception e) {
                    log.error("Failed to relay outbox event {}. Will retry. Error: {}", event.getId(), e.getMessage());
                    break; // Maintain temporal order; next tick will retry from here
                }
            }
        } finally {
            redisTemplate.delete(LOCK_KEY);
        }
    }
}
