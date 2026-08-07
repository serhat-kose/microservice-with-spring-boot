package com.serhat.ecommerce.stockservice.dto;

import com.serhat.ecommerce.stockservice.model.Stock;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public class StockDtos {

    public record CreateStockRequest(@NotBlank String productId, @Min(0) int quantity) {}

    public record UpdateStockRequest(@Min(0) int quantity) {}

    public record StockResponse(String productId, Integer quantity, Instant updatedAt) {
        public static StockResponse from(Stock stock) {
            return new StockResponse(stock.getProductId(), stock.getQuantity(), stock.getUpdatedAt());
        }
    }

    /**
     * Availability is reported in the body with a 200 rather than as a status code. The
     * previous "unavailable" response used 402 PAYMENT_REQUIRED, which has nothing to do
     * with inventory and misleads any client that reacts to status codes.
     */
    public record AvailabilityResponse(String productId, int requested, boolean available) {}
}
