package com.serhat.ecommerce.shipmentservice.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Parcel lifecycle. Previously the service only ever emitted {@code SCHEDULED} and nothing
 * tracked what happened next, so "where is my order" had no answer.
 */
public enum ShipmentStatus {

    /** Accepted and queued with the carrier. */
    SCHEDULED,
    /** Being picked and packed. */
    PREPARING,
    /** Handed to the carrier. */
    SHIPPED,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    /** Could not be dispatched; the saga refunds and releases stock. */
    FAILED,
    RETURNED;

    private static final Set<ShipmentStatus> TERMINAL = EnumSet.of(DELIVERED, FAILED, RETURNED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** Refuses moves that would run the parcel's history backwards. */
    public boolean canTransitionTo(ShipmentStatus target) {
        if (this == target) {
            return true;
        }
        if (isTerminal()) {
            return target == RETURNED && this == DELIVERED;
        }
        return switch (this) {
            case SCHEDULED -> target == PREPARING || target == SHIPPED || target == FAILED;
            case PREPARING -> target == SHIPPED || target == FAILED;
            case SHIPPED -> target == IN_TRANSIT || target == OUT_FOR_DELIVERY
                    || target == DELIVERED || target == FAILED;
            case IN_TRANSIT -> target == OUT_FOR_DELIVERY || target == DELIVERED || target == FAILED;
            case OUT_FOR_DELIVERY -> target == DELIVERED || target == FAILED;
            default -> false;
        };
    }
}
