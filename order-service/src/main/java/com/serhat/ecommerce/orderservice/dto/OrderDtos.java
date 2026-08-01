package com.serhat.ecommerce.orderservice.dto;

import com.serhat.ecommerce.orderservice.model.Order;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class OrderDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateOrderRequest {
        @NotBlank
        private String userId;
        @NotEmpty
        private List<Map<String, Object>> items;
        @NotNull
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCreatedEvent {
        private Long orderId;
        private String userId;
        private List<Map<String, Object>> items;
        private BigDecimal amount;
        private String status;
    }

    public record OrderResponse(
            Long id,
            String userId,
            BigDecimal amount,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static OrderResponse from(Order order) {
            return new OrderResponse(order.getId(), order.getUserId(), order.getAmount(),
                    order.getStatus(), order.getCreatedAt(), order.getUpdatedAt());
        }
    }
}
