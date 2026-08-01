package com.serhat.ecommerce.cartservice.dto;

import com.serhat.ecommerce.cartservice.model.Cart;
import com.serhat.ecommerce.cartservice.model.CartItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class CartDtos {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddItemRequest {
        @NotNull
        private Long productId;
        @NotNull
        @Min(1)
        private Integer quantity;
        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal price;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutEvent {
        private String userId;
        private List<CartItem> items;
        private BigDecimal total;
    }

    public record CartResponse(Long id, String userId, List<CartItem> items, BigDecimal total) {
        public static CartResponse from(Cart cart) {
            BigDecimal total = cart.getItems().stream()
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CartResponse(cart.getId(), cart.getUserId(), cart.getItems(), total);
        }
    }
}
