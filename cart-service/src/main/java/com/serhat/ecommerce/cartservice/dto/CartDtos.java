package com.serhat.ecommerce.cartservice.dto;

import com.serhat.ecommerce.cartservice.model.CartItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CartDtos {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddItemRequest {
        private String productId;
        private Integer quantity;
        private java.math.BigDecimal price;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutEvent {
        private String userId;
        private List<CartItem> items;
        private java.math.BigDecimal total;
    }
}

