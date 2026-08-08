package com.serhat.ecommerce.promotionservice.model;

public enum DiscountType {
    /** Takes a percentage off the order total, optionally capped. */
    PERCENTAGE,
    /** Takes a fixed amount off the order total. */
    FIXED_AMOUNT
}
