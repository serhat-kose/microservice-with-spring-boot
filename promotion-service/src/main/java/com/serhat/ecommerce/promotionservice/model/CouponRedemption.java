package com.serhat.ecommerce.promotionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One redemption of a coupon against one order.
 *
 * <p>The unique constraint on (couponId, orderId) is what makes redemption idempotent: a
 * retried checkout for the same order cannot consume the coupon twice, no matter how many
 * times the request arrives.
 */
@Entity
@Table(name = "coupon_redemptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_redemption_coupon_order",
                columnNames = {"coupon_id", "order_id"}),
        indexes = @Index(name = "idx_redemption_coupon_user", columnList = "coupon_id, user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false)
    private Instant redeemedAt;

    @PrePersist
    void prePersist() {
        if (redeemedAt == null) {
            redeemedAt = Instant.now();
        }
    }
}
