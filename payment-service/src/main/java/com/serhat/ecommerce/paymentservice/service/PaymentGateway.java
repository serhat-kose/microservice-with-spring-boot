package com.serhat.ecommerce.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stands in for a real payment provider.
 *
 * <p>Deliberately able to decline. The previous implementation returned a hardcoded
 * {@code "SUCCESS"} with no failure path at all, which meant the saga's entire compensation
 * half - releasing stock, refunding, failing the order - was unreachable code that had never
 * once executed. A configurable decline rate makes those paths exercisable, and declines are
 * genuine domain outcomes rather than exceptions, so they flow through the saga rather than
 * being retried as infrastructure faults.
 */
@Slf4j
@Component
public class PaymentGateway {

    /**
     * Share of authorisations that are declined, 0.0-1.0. Zero by default so ordinary use is
     * deterministic; raise it (e.g. via PAYMENT_DECLINE_RATE) to exercise the rollback path.
     */
    @Value("${ecommerce.payment.decline-rate:0.0}")
    private double declineRate;

    /** Authorisations above this amount are declined, standing in for a limit check. */
    @Value("${ecommerce.payment.max-amount:100000.00}")
    private BigDecimal maxAmount;

    public Authorization authorize(String orderId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return Authorization.declined("invalid_amount");
        }
        if (amount.compareTo(maxAmount) > 0) {
            return Authorization.declined("amount_exceeds_limit");
        }
        if (declineRate > 0 && ThreadLocalRandom.current().nextDouble() < declineRate) {
            return Authorization.declined("insufficient_funds");
        }
        return Authorization.approved("pay_" + UUID.randomUUID());
    }

    public String refund(String providerReference, BigDecimal amount) {
        log.info("Refunding {} against {}", amount, providerReference);
        return "ref_" + UUID.randomUUID();
    }

    public record Authorization(boolean approved, String reference, String declineReason) {

        static Authorization approved(String reference) {
            return new Authorization(true, reference, null);
        }

        static Authorization declined(String reason) {
            return new Authorization(false, null, reason);
        }
    }
}
