package com.example.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerId;

    // items JSON (ör: [{"productId":"p1","quantity":2}, ...])
    @Lob
    @Column(columnDefinition = "text")
    private String items;



    @Column(nullable = false)
    private String status; // PENDING, RESERVED, PAID, SHIPPED, COMPLETED, FAILED

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) this.status = "PENDING";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
