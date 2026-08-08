package com.serhat.ecommerce.cartservice.dto;

import com.serhat.ecommerce.cartservice.model.Cart;
import com.serhat.ecommerce.cartservice.model.CartItem;
import com.serhat.ecommerce.cartservice.service.PricingService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

    public record ApplyCouponRequest(@NotBlank String code) {}

    public record AddressDto(
            @NotBlank String recipientName,
            @NotBlank String phone,
            @NotBlank String line1,
            String line2,
            @NotBlank String city,
            String district,
            @NotBlank String postalCode,
            @NotBlank String country
    ) {}

    /** Checkout needs a destination; the address is supplied here and snapshotted onto the order. */
    public record CheckoutRequest(@NotNull AddressDto shippingAddress) {}

    public record CartItemResponse(Long productId, String productName, Integer quantity,
                                   BigDecimal unitPrice, BigDecimal lineTotal) {
        public static CartItemResponse from(CartItem item) {
            return new CartItemResponse(item.getProductId(), item.getProductName(),
                    item.getQuantity(), item.getPrice(),
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
    }

    /** What the cart page renders: the lines plus the full price breakdown. */
    public record CartResponse(
            Long id,
            String userId,
            List<CartItemResponse> items,
            String couponCode,
            String couponMessage,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal shippingCost,
            BigDecimal taxAmount,
            BigDecimal total
    ) {
        public static CartResponse from(Cart cart, PricingService.Quote quote, String couponMessage) {
            return new CartResponse(
                    cart.getId(), cart.getUserId(),
                    cart.getItems().stream().map(CartItemResponse::from).toList(),
                    quote.getCouponCode(), couponMessage,
                    quote.getSubtotal(), quote.getDiscountAmount(),
                    quote.getShippingCost(), quote.getTaxAmount(), quote.getTotalAmount());
        }
    }

    /** Payload published to start the checkout saga. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutEvent {
        private String userId;
        private List<CartItem> items;
        private AddressDto shippingAddress;
        private String couponCode;
        private BigDecimal discountAmount;
        private BigDecimal shippingCost;
        private BigDecimal taxAmount;
        private BigDecimal total;
    }
}
