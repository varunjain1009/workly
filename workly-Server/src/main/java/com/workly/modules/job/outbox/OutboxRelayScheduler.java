package com.workly.modules.job.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void relayEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (pendingEvents.isEmpty())
            return;

        log.debug("Found {} pending outbox events to process", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Send to Kafka synchronously (or wait for future) to ensure durability
                kafkaTemplate.send(event.getTopic(), event.getPayload()).get();

                // Mark as processed
                event.setProcessed(true);
                outboxEventRepository.save(event);
                log.debug("Successfully relayed outbox event {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to relay outbox event {}. Will retry. Error: {}", event.getId(), e.getMessage());
                // Stop processing further events to maintain order, or continue?
                // For MVP, we can continue or break. Let's break to maintain temporal order.
                break;
            }
        }
    }
}
