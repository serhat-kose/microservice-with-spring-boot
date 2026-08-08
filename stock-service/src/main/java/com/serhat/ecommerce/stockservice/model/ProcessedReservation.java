package com.serhat.ecommerce.stockservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A stock reservation held for one order.
 *
 * <p>Serves two purposes. As an idempotency marker it stops a redelivered
 * {@code stock-reserve-request} from decrementing stock twice - Kafka only guarantees
 * at-least-once delivery. As a record of what was held, it also lets a reservation be
 * released later: without the reserved quantities stored here, an abandoned saga would leak
 * stock permanently, because nothing would know what to give back.
 */
@Entity
@Table(name = "processed_reservations", indexes = {
        @Index(name = "idx_reservations_state", columnList = "state, reservedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedReservation {

    @Id
    private String orderId;

    @Column(nullable = false)
    private Instant reservedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationState state = ReservationState.HELD;

    /**
     * What was reserved, as compact JSON ({@code [{"productId":"x","quantity":n}]}).
     * Stored rather than re-derived from the order because releasing must give back exactly
     * what was taken, even if the order has since changed.
     */
    @Lob
    @Column(columnDefinition = "text")
    private String reservedItems;

    private Instant releasedAt;

    @PrePersist
    void prePersist() {
        if (reservedAt == null) {
            reservedAt = Instant.now();
        }
    }
}
