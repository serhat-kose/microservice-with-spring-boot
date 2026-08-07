package com.serhat.ecommerce.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An entry in a customer's address book. Orders snapshot the address at checkout time
 * rather than referencing this row, so later edits here never rewrite past orders.
 */
@Entity
@Table(name = "user_addresses", indexes = {
        @Index(name = "idx_user_addresses_user_id", columnList = "userId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** Customer-chosen label, e.g. "Home", "Office". */
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String line1;

    private String line2;

    @Column(nullable = false)
    private String city;

    private String district;

    @Column(nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    @Builder.Default
    private boolean defaultAddress = false;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
