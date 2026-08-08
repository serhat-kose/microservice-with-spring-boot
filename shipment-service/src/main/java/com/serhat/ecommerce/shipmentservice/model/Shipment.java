package com.serhat.ecommerce.shipmentservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A shipment for one order.
 *
 * <p>shipment-service used to hold nothing: it emitted a random UUID as a "shipmentId" and
 * threw it away, so a customer could never be told where their parcel was and nothing could
 * be looked up afterwards. The unique constraint on orderId also makes a redelivered
 * {@code shipment-request} a no-op rather than a second dispatch.
 */
@Entity
@Table(name = "shipments",
        uniqueConstraints = @UniqueConstraint(name = "uk_shipments_order", columnNames = "order_id"),
        indexes = {
                @Index(name = "idx_shipments_tracking", columnList = "trackingNumber"),
                @Index(name = "idx_shipments_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    private String userId;

    /** The number a customer quotes to the carrier to trace the parcel. */
    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Carrier carrier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(length = 1000)
    private String deliveryAddress;

    private String failureReason;

    private Instant estimatedDelivery;
    private Instant deliveredAt;

    private Instant createdAt;
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
