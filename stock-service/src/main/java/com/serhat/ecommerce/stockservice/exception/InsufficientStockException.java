package com.serhat.ecommerce.stockservice.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productId) {
        super("Insufficient stock for product: " + productId);
    }
}
