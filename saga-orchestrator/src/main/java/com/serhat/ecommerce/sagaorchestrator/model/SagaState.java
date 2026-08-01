package com.serhat.ecommerce.sagaorchestrator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Durable record of where a single order's saga currently stands, keyed by orderId.
 * Without this, the orchestrator was a pure in-flight event router with no way to
 * inspect, resume, or detect a stuck saga after a crash.
 */
@Entity
@Table(name = "saga_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaState {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
