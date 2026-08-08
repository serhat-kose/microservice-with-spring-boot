package com.serhat.ecommerce.cartservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "carts", indexes = @Index(name = "idx_carts_user", columnList = "userId", unique = true))
@Data
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_items", joinColumns = @JoinColumn(name = "cart_id"))
    private List<CartItem> items = new ArrayList<>();

    /**
     * The coupon the shopper has applied. Only the code is kept - the discount it is worth
     * depends on the basket, so it is re-quoted from promotion-service rather than stored
     * and allowed to go stale as items are added or removed.
     */
    private String couponCode;

    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
