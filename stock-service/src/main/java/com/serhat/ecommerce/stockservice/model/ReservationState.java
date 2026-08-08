package com.serhat.ecommerce.stockservice.model;

public enum ReservationState {
    /** Stock is decremented and being held for an in-flight order. */
    HELD,
    /** The order completed; the held stock was genuinely sold. */
    CONFIRMED,
    /** The hold was given back, either by the saga's compensation or by the TTL sweeper. */
    RELEASED
}
