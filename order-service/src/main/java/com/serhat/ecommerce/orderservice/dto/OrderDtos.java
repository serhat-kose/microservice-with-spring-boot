package com.serhat.ecommerce.orderservice.dto;

import com.serhat.ecommerce.orderservice.model.Order;
import com.serhat.ecommerce.orderservice.model.OrderItem;
import com.serhat.ecommerce.orderservice.model.OrderStatus;
import com.serhat.ecommerce.orderservice.model.ShippingAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderDtos {

    public record AddressDto(
            @NotBlank String recipientName,
            @NotBlank String phone,
            @NotBlank String line1,
            String line2,
            @NotBlank String city,
            String district,
            @NotBlank String postalCode,
            @NotBlank String country
    ) {
        public ShippingAddress toEntity() {
            return ShippingAddress.builder()
                    .recipientName(recipientName).phone(phone)
                    .line1(line1).line2(line2)
                    .city(city).district(district)
                    .postalCode(postalCode).country(country)
                    .build();
        }

        public static AddressDto from(ShippingAddress a) {
            if (a == null) {
                return null;
            }
            return new AddressDto(a.getRecipientName(), a.getPhone(), a.getLine1(), a.getLine2(),
                    a.getCity(), a.getDistrict(), a.getPostalCode(), a.getCountry());
        }
    }

    public record OrderItemRequest(
            @NotNull Long productId,
            String sku,
            @NotBlank String productName,
            @NotNull @Min(1) Integer quantity,
            @NotNull BigDecimal unitPrice
    ) {}

    /**
     * Mutable because the controller overwrites {@code userId} with the authenticated
     * caller before the request is published - the client's own value is never trusted.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {
        private String userId;
        @NotEmpty
        private List<@Valid OrderItemRequest> items;
        @Valid
        private AddressDto shippingAddress;
        private String couponCode;
        private BigDecimal discountAmount;
        private BigDecimal shippingCost;
        private BigDecimal taxAmount;
    }

    public record OrderItemResponse(Long productId, String sku, String productName,
                                    Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getProductId(), item.getSku(), item.getProductName(),
                    item.getQuantity(), item.getUnitPrice(), item.lineTotal());
        }
    }

    /** Listing shape - no lines, so an order history page stays cheap. */
    public record OrderSummary(
            Long id,
            OrderStatus status,
            BigDecimal totalAmount,
            int itemCount,
            Instant createdAt
    ) {
        public static OrderSummary from(Order order) {
            return new OrderSummary(order.getId(), order.getStatus(), order.getTotalAmount(),
                    order.getItems().size(), order.getCreatedAt());
        }
    }

    /** Detail shape - includes the lines, which no endpoint previously returned at all. */
    public record OrderDetail(
            Long id,
            String userId,
            OrderStatus status,
            String failureReason,
            List<OrderItemResponse> items,
            AddressDto shippingAddress,
            BigDecimal subtotal,
            BigDecimal discountAmount,
            String couponCode,
            BigDecimal shippingCost,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static OrderDetail from(Order o) {
            return new OrderDetail(o.getId(), o.getUserId(), o.getStatus(), o.getFailureReason(),
                    o.getItems().stream().map(OrderItemResponse::from).toList(),
                    AddressDto.from(o.getShippingAddress()),
                    o.getSubtotal(), o.getDiscountAmount(), o.getCouponCode(),
                    o.getShippingCost(), o.getTaxAmount(), o.getTotalAmount(),
                    o.getCreatedAt(), o.getUpdatedAt());
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size,
                                  long totalElements, int totalPages, boolean last) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.isLast());
        }
    }
}
