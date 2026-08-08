package com.serhat.ecommerce.orderservice.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Order lifecycle, with the transitions that are actually allowed.
 *
 * <p>Status used to be a free-form string written by whichever event arrived last, so a
 * late-delivered {@code stock-reserved} could drag a COMPLETED order back to RESERVED. The
 * transition table below makes those moves impossible: anything not listed is refused.
 */
public enum OrderStatus {

    CREATED,
    /** Stock has been held for the order. */
    RESERVED,
    PAID,
    /** Handed to the shipment step. */
    SHIPPED,
    COMPLETED,
    FAILED,
    CANCELLED;

    private static final Set<OrderStatus> TERMINAL = EnumSet.of(COMPLETED, FAILED, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /**
     * Whether the order may move from this status to {@code target}.
     *
     * <p>Re-entering the same status is allowed so that a redelivered event is a harmless
     * no-op rather than an error, while going backwards or leaving a terminal status is not.
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (this == target) {
            return true;
        }
        if (isTerminal()) {
            return false;
        }
        return switch (this) {
            case CREATED -> target == RESERVED || target == FAILED || target == CANCELLED;
            case RESERVED -> target == PAID || target == FAILED || target == CANCELLED;
            case PAID -> target == SHIPPED || target == COMPLETED || target == FAILED;
            case SHIPPED -> target == COMPLETED || target == FAILED;
            default -> false;
        };
    }
}
