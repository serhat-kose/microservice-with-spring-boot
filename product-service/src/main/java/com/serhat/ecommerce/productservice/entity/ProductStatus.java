package com.serhat.ecommerce.productservice.entity;

public enum ProductStatus {
    /** Being prepared by the seller; never visible to shoppers. */
    DRAFT,
    /** Listed and purchasable. */
    ACTIVE,
    /** Withdrawn from sale but retained so existing orders still resolve it. */
    INACTIVE
}
