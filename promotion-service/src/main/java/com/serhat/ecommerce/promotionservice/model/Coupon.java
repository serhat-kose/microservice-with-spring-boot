package com.serhat.ecommerce.promotionservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupons_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stored upper-cased so lookups are case-insensitive without a functional index. */
    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    /** Percentage (0-100) or a fixed amount, depending on {@link #discountType}. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    /** Caps how much a percentage coupon can take off; ignored for fixed-amount coupons. */
    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(nullable = false)
    private Instant validFrom;

    @Column(nullable = false)
    private Instant validUntil;

    /** Total redemptions allowed across all customers; null means unlimited. */
    private Integer usageLimit;

    /** How many times one customer may redeem it; null means unlimited. */
    private Integer perUserLimit;

    /**
     * Incremented atomically on redemption. Kept as a column rather than derived by counting
     * redemptions so the usage limit can be enforced with a single conditional UPDATE.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (code != null) {
            code = code.toUpperCase();
        }
    }

    @PreUpdate
    void preUpdate() {
        if (code != null) {
            code = code.toUpperCase();
        }
    }

    /**
     * Discount for a given order total, never more than the total itself - a fixed-amount
     * coupon larger than the basket must not produce a negative payable amount.
     */
    public BigDecimal discountFor(BigDecimal orderAmount) {
        BigDecimal discount = discountType == DiscountType.PERCENTAGE
                ? orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : discountValue;

        if (discountType == DiscountType.PERCENTAGE && maxDiscountAmount != null) {
            discount = discount.min(maxDiscountAmount);
        }
        return discount.min(orderAmount).setScale(2, RoundingMode.HALF_UP);
    }
}
