package com.serhat.ecommerce.paymentservice.service;

import com.serhat.ecommerce.paymentservice.model.Payment;
import com.serhat.ecommerce.paymentservice.model.PaymentMethod;
import com.serhat.ecommerce.paymentservice.model.PaymentStatus;
import com.serhat.ecommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway gateway;

    /**
     * Charges an order, at most once.
     *
     * <p>If a payment already exists for the order the stored outcome is returned unchanged -
     * a redelivered {@code payment-request} must not become a second charge, and Kafka
     * guarantees only at-least-once delivery.
     */
    @Transactional
    public Payment charge(String orderId, String userId, BigDecimal amount, PaymentMethod method) {
        Optional<Payment> existing = paymentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.debug("Payment for order {} already exists with status {}",
                    orderId, existing.get().getStatus());
            return existing.get();
        }

        PaymentGateway.Authorization authorization = gateway.authorize(orderId, amount);

        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount == null ? BigDecimal.ZERO : amount)
                .method(method == null ? PaymentMethod.CREDIT_CARD : method)
                .status(authorization.approved() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .providerReference(authorization.reference())
                .failureReason(authorization.declineReason())
                .build();

        Payment saved = paymentRepository.save(payment);
        if (!authorization.approved()) {
            log.info("Payment declined for order {}: {}", orderId, authorization.declineReason());
        }
        return saved;
    }

    /**
     * Refunds a successful payment, at most once.
     *
     * <p>Returns the payment either way so the caller can always publish a result - the saga
     * waits for {@code payment-refund-result} to consider itself compensated, so staying
     * silent when there is nothing to refund would leave it hanging.
     */
    @Transactional
    public Optional<Payment> refund(String orderId) {
        Optional<Payment> found = paymentRepository.findByOrderId(orderId);
        if (found.isEmpty()) {
            log.warn("Refund requested for order {} with no payment on record", orderId);
            return Optional.empty();
        }

        Payment payment = found.get();
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return Optional.of(payment);
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            // Nothing was ever taken, so there is nothing to give back.
            return Optional.of(payment);
        }

        gateway.refund(payment.getProviderReference(), payment.getAmount());
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAmount(payment.getAmount());
        return Optional.of(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }
}
