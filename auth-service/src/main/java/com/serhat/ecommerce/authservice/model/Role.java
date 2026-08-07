package com.serhat.ecommerce.authservice.model;

/**
 * Roles the platform recognises. Carried in the JWT and forwarded downstream by the
 * gateway as the {@code X-User-Roles} header.
 */
public enum Role {
    /** Ordinary shopper: owns a cart, places orders, writes reviews. */
    CUSTOMER,
    /** Merchant: manages their own products and stock. */
    SELLER,
    /** Platform operator: full catalog, stock and promotion management. */
    ADMIN
}
