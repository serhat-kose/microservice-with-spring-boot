package com.serhat.ecommerce.cartservice.dto;

import com.serhat.ecommerce.cartservice.model.Cart;
import com.serhat.ecommerce.cartservice.model.CartItem;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class CartDtos {

    /**
     * Deliberately carries no price. The price previously came from the client and was
     * stored verbatim, so a shopper could add an item at any price they chose and check out
     * at it. It is now resolved server-side from the catalog read-model.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddItemRequest {
        @NotNull
        private Long productId;
        @NotNull
        @Min(1)
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutEvent {
        private String userId;
        private List<CartItem> items;
        private BigDecimal total;
    }

    public record CartItemResponse(Long productId, String productName, Integer quantity,
                                   BigDecimal unitPrice, BigDecimal lineTotal) {
        public static CartItemResponse from(CartItem item) {
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            return new CartItemResponse(item.getProductId(), item.getProductName(),
                    item.getQuantity(), item.getPrice(), lineTotal);
        }
    }

    public record CartResponse(Long id, String userId, List<CartItemResponse> items, BigDecimal total) {
        public static CartResponse from(Cart cart) {
            List<CartItemResponse> items = cart.getItems().stream()
                    .map(CartItemResponse::from)
                    .toList();
            BigDecimal total = items.stream()
                    .map(CartItemResponse::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CartResponse(cart.getId(), cart.getUserId(), items, total);
        }
    }
}
