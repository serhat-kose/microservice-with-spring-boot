package com.serhat.ecommerce.sagaorchestrator.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where an order's saga currently stands.
 *
 * <p>Statuses were free-form strings written by whichever event arrived last. With no
 * ordering guarantee between topics, a redelivered {@code order-created} could re-request a
 * reservation for an order already being shipped. The transition table below refuses moves
 * that would take a saga backwards or out of a terminal state.
 */
public enum SagaStatus {

    STARTED,
    STOCK_RESERVATION_REQUESTED,
    PAYMENT_REQUESTED,
    SHIPMENT_REQUESTED,
    COMPLETED,
    FAILED_INSUFFICIENT_STOCK,
    /** Payment failed; stock is being handed back. */
    FAILED_PAYMENT_COMPENSATING,
    /** Shipment failed; payment is being refunded. */
    FAILED_SHIPMENT_COMPENSATING,
    /** Compensation finished - the saga is closed. */
    FAILED_COMPENSATED,
    /** Gave up after sitting in one step past the timeout. */
    TIMED_OUT;

    private static final Set<SagaStatus> TERMINAL =
            EnumSet.of(COMPLETED, FAILED_INSUFFICIENT_STOCK, FAILED_COMPENSATED, TIMED_OUT);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** True while the saga is waiting on a step and could get stuck there. */
    public boolean isInFlight() {
        return !isTerminal();
    }

    public boolean canTransitionTo(SagaStatus target) {
        if (this == target) {
            return true;
        }
        if (isTerminal()) {
            return false;
        }
        return switch (this) {
            case STARTED -> target == STOCK_RESERVATION_REQUESTED || isFailure(target);
            case STOCK_RESERVATION_REQUESTED -> target == PAYMENT_REQUESTED || isFailure(target);
            case PAYMENT_REQUESTED -> target == SHIPMENT_REQUESTED || isFailure(target);
            case SHIPMENT_REQUESTED -> target == COMPLETED || isFailure(target);
            // A compensating saga may only move on to being fully compensated, or time out.
            case FAILED_PAYMENT_COMPENSATING, FAILED_SHIPMENT_COMPENSATING ->
                    target == FAILED_COMPENSATED || target == TIMED_OUT;
            default -> false;
        };
    }

    private static boolean isFailure(SagaStatus target) {
        return target == FAILED_INSUFFICIENT_STOCK
                || target == FAILED_PAYMENT_COMPENSATING
                || target == FAILED_SHIPMENT_COMPENSATING
                || target == TIMED_OUT;
    }
}
