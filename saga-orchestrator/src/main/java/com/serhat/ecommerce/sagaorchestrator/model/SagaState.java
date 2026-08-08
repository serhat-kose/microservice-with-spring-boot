package com.serhat.ecommerce.sagaorchestrator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Durable record of where a single order's saga stands.
 *
 * <p>Beyond letting a stalled saga be found, the stored context is what makes compensation
 * possible at all: when payment fails, the stock to release and the amount to refund have to
 * come from somewhere, and the failing event does not carry them. Previously the orchestrator
 * read them off whatever message triggered the failure, which is why the compensation paths
 * would have thrown a NullPointerException the first time they ran.
 */
@Entity
@Table(name = "saga_states", indexes = {
        @Index(name = "idx_saga_status_updated", columnList = "status, updatedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaState {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStatus status;

    private String userId;

    /** Order total, kept so a refund can be issued without the shipment event carrying it. */
    private String amount;

    /** Reserved lines as JSON, kept so stock can be released without the payment event carrying them. */
    @Lob
    @Column(columnDefinition = "text")
    private String items;

    private String lastError;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    /**
     * Two listener threads can process events for the same order concurrently, so the status
     * write needs a concurrency guard rather than last-write-wins.
     */
    @Version
    private Long version;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
