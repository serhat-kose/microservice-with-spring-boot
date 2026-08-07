package com.serhat.ecommerce.productservice.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A domain event awaiting publication, written in the same transaction as the change that
 * produced it (the transactional outbox pattern).
 *
 * <p>This is what makes "the database changed but the event was lost" impossible: both the
 * catalog write and this row commit together, and a separate relay drains the table. The
 * cost is at-least-once delivery - the relay can publish and then fail before marking the
 * row sent - which is why every consumer is idempotent on {@code eventId}.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_unpublished", columnList = "published, createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    /** Kafka message key - the aggregate id, so events for one product stay ordered. */
    @Column(name = "message_key")
    private String messageKey;

    @Lob
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    private String lastError;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
