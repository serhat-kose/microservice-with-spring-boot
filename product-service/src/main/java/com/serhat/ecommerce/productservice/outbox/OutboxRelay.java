package com.serhat.ecommerce.productservice.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Drains the outbox to Kafka.
 *
 * <p>Publishes are confirmed synchronously before a row is marked sent, so a broker failure
 * leaves the event pending and it is retried on the next tick instead of being lost. A crash
 * between the successful publish and the status update causes a redelivery, which is safe
 * because consumers deduplicate on the envelope's {@code eventId}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${ecommerce.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch = repository.lockPendingBatch(PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) {
            return;
        }

        for (OutboxEvent event : batch) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);
                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
            } catch (Exception e) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(truncate(e.getMessage()));
                if (event.getAttempts() >= MAX_ATTEMPTS) {
                    // Left unpublished and loudly logged rather than dropped: a permanently
                    // failing event means a consumer's read-model is drifting and needs a human.
                    log.error("Outbox event {} on topic {} has failed {} times and needs attention",
                            event.getId(), event.getTopic(), event.getAttempts(), e);
                } else {
                    log.warn("Failed to publish outbox event {} (attempt {}), will retry",
                            event.getId(), event.getAttempts());
                }
            }
        }
        repository.saveAll(batch);
    }

    /** Keeps the table from growing without bound once events have been delivered. */
    @Scheduled(cron = "${ecommerce.outbox.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void purgePublished() {
        repository.deleteByPublishedTrueAndPublishedAtBefore(Instant.now().minus(7, ChronoUnit.DAYS));
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
