package com.serhat.ecommerce.cartservice.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private String productId;
    private Integer quantity;
    private BigDecimal price;
}

