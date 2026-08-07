package com.serhat.ecommerce.productservice.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Object key) {
        super("Product not found: " + key);
    }
}
