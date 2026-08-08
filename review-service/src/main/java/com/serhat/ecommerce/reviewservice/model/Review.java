package com.serhat.ecommerce.reviewservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A customer's review of a product.
 *
 * <p>The unique constraint on (productId, userId) enforces one review per customer per
 * product at the database level rather than only in service code, so concurrent submissions
 * cannot both slip through.
 */
@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_reviews_product_user",
                columnNames = {"product_id", "user_id"}),
        indexes = {
                @Index(name = "idx_reviews_product", columnList = "product_id, status"),
                @Index(name = "idx_reviews_user", columnList = "user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Denormalised so listing reviews does not require a call into auth-service per row. */
    private String authorName;

    @Column(nullable = false)
    private Integer rating;

    private String title;

    @Column(length = 4000)
    private String body;

    /**
     * True when the reviewer's purchase of this product was confirmed from an order event.
     * Only verified reviews count towards the product's average, which is what stops the
     * rating being moved by accounts that never bought the item.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean verifiedPurchase = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

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
