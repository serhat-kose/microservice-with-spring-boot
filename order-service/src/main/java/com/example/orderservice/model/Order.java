package com.example.orderservice.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

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

    private String userId;

    @Lob
    @Column(columnDefinition = "text")
    private String itemsJson; // JSON serialized list of items

    private BigDecimal amount;

    private String status; // CREATED, PROCESSING, COMPLETED, FAILED

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = Instant.now();
        if (this.status == null) this.status = "CREATED";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
