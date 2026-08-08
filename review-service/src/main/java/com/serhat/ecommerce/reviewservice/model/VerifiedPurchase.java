package com.serhat.ecommerce.reviewservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Record that a customer actually received a given product, built from completed-order
 * events. This is what a review is checked against before it is accepted.
 *
 * <p>It is kept here rather than queried from order-service on demand so that reviewing does
 * not depend on order-service being reachable, and so the check is a local index lookup
 * instead of a cross-service call on every submission.
 */
@Entity
@Table(name = "verified_purchases",
        uniqueConstraints = @UniqueConstraint(name = "uk_verified_purchase",
                columnNames = {"user_id", "product_id"}),
        indexes = @Index(name = "idx_verified_purchase_lookup", columnList = "user_id, product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifiedPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private Instant purchasedAt;

    @PrePersist
    void prePersist() {
        if (purchasedAt == null) {
            purchasedAt = Instant.now();
        }
    }
}
