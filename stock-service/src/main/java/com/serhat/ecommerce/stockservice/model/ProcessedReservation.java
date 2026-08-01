package com.serhat.ecommerce.stockservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Idempotency marker: one row per orderId whose stock reservation has already been
 * applied. Kafka only guarantees at-least-once delivery, so under load (consumer
 * rebalances, retries after a transient failure) the same "stock-reserve-request"
 * message can be redelivered - without this, that would decrement stock twice for
 * the same order.
 */
@Entity
@Table(name = "processed_reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedReservation {

    @Id
    private String orderId;

    private Instant reservedAt;
}
