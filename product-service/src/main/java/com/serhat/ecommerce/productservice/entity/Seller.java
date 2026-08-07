package com.serhat.ecommerce.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A merchant selling on the marketplace.
 *
 * <p>{@code userId} links back to the account in auth-service that administers this seller,
 * which is what lets a SELLER be restricted to their own products rather than the whole
 * catalog. It is a plain value, not a foreign key - services own their own schema and
 * auth-service's tables live in a different database.
 */
@Entity
@Table(name = "sellers", indexes = {
        @Index(name = "idx_sellers_slug", columnList = "slug", unique = true),
        @Index(name = "idx_sellers_user_id", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    /** The auth-service user id that owns this storefront. */
    @Column(nullable = false)
    private String userId;

    /** Rolling average of the seller's rating, maintained from review events. */
    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
