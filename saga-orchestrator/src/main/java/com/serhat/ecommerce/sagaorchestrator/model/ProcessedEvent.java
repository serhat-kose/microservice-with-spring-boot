package com.serhat.ecommerce.sagaorchestrator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Records which events this orchestrator has already acted on.
 *
 * <p>Kafka is at-least-once, and a rebalance or a retry after a transient failure replays
 * messages. Without this, a redelivered {@code stock-reserved} would issue a second
 * {@code payment-request} - i.e. charge the customer twice - and a redelivered
 * {@code order-created} would reserve stock again. Recorded in the same transaction as the
 * saga step, so a crash cannot leave the step applied but unrecorded.
 */
@Entity
@Table(name = "processed_events", indexes = @Index(name = "idx_processed_at", columnList = "processedAt"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    /** The envelope's eventId. */
    @Id
    @Column(name = "event_id")
    private String eventId;

    private String eventType;

    private String orderId;

    @Column(nullable = false)
    private Instant processedAt;

    @PrePersist
    void prePersist() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}
