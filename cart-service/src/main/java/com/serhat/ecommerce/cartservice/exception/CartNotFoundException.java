package com.serhat.ecommerce.cartservice.exception;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String userId) {
        super("Cart not found for user: " + userId);
    }
}
