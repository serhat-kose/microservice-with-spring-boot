package com.serhat.ecommerce.sagaorchestrator.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.sagaorchestrator.model.SagaState;
import com.serhat.ecommerce.sagaorchestrator.model.SagaStatus;
import com.serhat.ecommerce.sagaorchestrator.service.SagaStateService;
import com.serhat.ecommerce.sagaorchestrator.service.SagaStateService.SagaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Drives the checkout saga.
 *
 * <p>Every handler follows the same shape: skip the event if it has already been applied,
 * attempt the state transition, and only publish the next command if that transition was
 * accepted. Doing it in that order is what makes redelivery safe - previously a replayed
 * {@code stock-reserved} issued a second payment request, i.e. charged the customer twice.
 *
 * <p>Compensation reads the amount and item list from the saga record rather than from the
 * failing event. The events that trigger compensation do not carry them, so the old code
 * would have thrown a NullPointerException the first time a payment actually failed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Orchestrator {

    private final SagaCommandPublisher publisher;
    private final SagaStateService sagaStateService;
    private final ObjectMapper objectMapper;

    /** Entry point: a checkout becomes an order-create command. */
    @KafkaListener(topics = "cart-checkout", groupId = "saga-orchestrator")
    public void onCartCheckout(String message) throws Exception {
        // Forwarded as-is; the order does not exist yet, so there is no saga to key on.
        JsonNode root = objectMapper.readTree(message);
        publisher.publishRaw("order-create", null, message);
        log.debug("Forwarded checkout to order-create");
    }

    @KafkaListener(topics = "order-created", groupId = "saga-orchestrator")
    @Transactional
    public void onOrderCreated(String message) throws Exception {
        Envelope envelope = envelope(message);
        String orderId = required(envelope.payload(), "orderId");
        if (skip(envelope, orderId)) {
            return;
        }

        String items = writeJson(envelope.payload().get("items"));
        SagaContext context = SagaContext.of(
                text(envelope.payload(), "userId"), text(envelope.payload(), "amount"), items);

        Optional<SagaState> moved = sagaStateService.transition(
                orderId, SagaStatus.STOCK_RESERVATION_REQUESTED, context);
        sagaStateService.markProcessed(envelope.eventId(), envelope.eventType(), orderId);
        if (moved.isEmpty()) {
            return;
        }

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("orderId", orderId);
        command.put("userId", moved.get().getUserId());
        command.put("amount", moved.get().getAmount());
        command.put("items", readJson(moved.get().getItems()));
        publisher.publish("stock-reserve-request", orderId, "stock.reserve.requested", command);
    }

    @KafkaListener(topics = "stock-reserved", groupId = "saga-orchestrator")
    @Transactional
    public void onStockReserved(String message) throws Exception {
        Envelope envelope = envelope(message);
        String orderId = required(envelope.payload(), "orderId");
        if (skip(envelope, orderId)) {
            return;
        }

        Optional<SagaState> moved = sagaStateService.transition(
                orderId, SagaStatus.PAYMENT_REQUESTED, SagaContext.empty());
        sagaStateService.markProcessed(envelope.eventId(), envelope.eventType(), orderId);
        if (moved.isEmpty()) {
            return;
        }

        Map<String, Object> command = new LinkedHashMap<>();
        command.put("orderId", orderId);
        command.put("userId", moved.get().getUserId());
        command.put("amount", moved.get().getAmount());
        command.put("items", readJson(moved.get().getItems()));
        publisher.publish("payment-request", orderId, "payment.requested", command);
    }

    @KafkaListener(topics = "stock-reservation-failed", groupId = "saga-orchestrator")
    @Transactional
    public void onStockReservationFailed(String message) throws Exception {
        Envelope envelope = envelope(message);
        String orderId = required(envelope.payload(), "orderId");
        if (skip(envelope, orderId)) {
            return;
        }

        Optional<SagaState> moved = sagaStateService.transition(orderId,
                SagaStatus.FAILED_INSUFFICIENT_STOCK, SagaContext.error("insufficient_stock"));
        sagaStateService.markProcessed(envelope.eventId(), envelope.eventType(), orderId);
        if (moved.isEmpty()) {
            return;
        }
        // Nothing was reserved, so there is nothing to compensate - only the order to fail.
        publishOrderFailed(orderId, moved.get().getUserId(), "insufficient_stock");
    }

    @KafkaListener(topics = "payment-result", groupId = "saga-orchestrator")
    @Transactional
    public void onPaymentResult(String message) throws Exception {
        Envelope envelope = envelope(message);
        String orderId = required(envelope.payload(), "orderId");
        if (skip(envelope, orderId)) {
            return;
        }

        boolean success = "SUCCESS".equalsIgnoreCase(text(envelope.payload(), "status"));

        if (success) {
            Optional<SagaState> moved = sagaStateService.transition(
                    orderId, SagaStatus.SHIPMENT_REQUESTED, SagaContext.empty());
            sagaStateService.markProcessed(envelope.eventId(), envelope.eventType(), orderId);
            if (moved.isEmpty()) {
                return;
            }

            Map<String, Object> command = new LinkedHashMap<>();
            command.put("orderId", orderId);
            command.put("userId", moved.get().getUserId());
            command.put("address", text(envelope.payload(), "address"));
            publisher.publish("shipment-request", orderId, "shipment.requested", command);
            return;
        }

        String reason = Optional.ofNullable(text(envelope.payload(), "reason")).orElse("payment_failed");
        Optional<SagaState> moved = sagaStateService.transition(
                orderId, SagaStatus.FAILED_PAYMENT_COMPENSATING, SagaContext.error(reason));
        sagaStateService.markProcessed(envelope.eventId(), envelope.eventType(), orderId);
        if (moved.isEmpty()) {
            return;
        }
        compensateStock(moved.get(), reason);
    }

    @KafkaListener(topics = "shipment-result", groupId = "saga-orchestrator")
    @Transactional
    public void onShipmentResult(String message) throws Exception {
        Envelope envelope = envelope(message);
        String orderId = required(envelope.payload(), "orderId");
        if (skip(envelope, orderId)) {
            return;
        }

        String status = text(envelope.payload(), "status");
        boolean scheduled = "SCHEDULED".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);

        if (scheduled) {
            Optional<SagaState> moved = sagaStateService.transition(
                    orderId, SagaStatus.COMPLETED, SagaContext.empty());
            sagaStateService.markProcessed(envelope.eventId(), envelope.eventType(), orderId);
            if (moved.isEmpty()) {
                return;
            }

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("orderId", orderId);
            event.put("userId", moved.get().getUserId());
            event.put("items", readJson(moved.get().getItems()));
            event.put("amount", moved.get().getAmount());
            event.put("status", "COMPLETED");
            publisher.publish("order-completed", orderId, "order.completed", event);
            return;
        }

        String reason = Optional.ofNullable(text(envelope.payload(), "reason")).orElse("shipment_failed");
        Optional<SagaState> moved = sagaStateService.transition(
                orderId, SagaStatus.FAILED_SHIPMENT_COMPENSATING, SagaContext.error(reason));
        sagaStateService.markProcessed(envelope.eventId(), envelope.eventType(), orderId);
        if (moved.isEmpty()) {
            return;
        }
        compensateShipment(moved.get(), reason);
    }

    /**
     * Closes the loop on a refund. Without consuming this, a saga that had to refund sat in
     * FAILED_SHIPMENT_COMPENSATING forever and the watchdog would eventually flag it as
     * stuck even though the compensation had succeeded.
     */
    @KafkaListener(topics = "payment-refund-result", groupId = "saga-orchestrator")
    @Transactional
    public void onRefundResult(String message) throws Exception {
        Envelope envelope = envelope(message);
        String orderId = required(envelope.payload(), "orderId");
        if (skip(envelope, orderId)) {
            return;
        }
        sagaStateService.transition(orderId, SagaStatus.FAILED_COMPENSATED, SagaContext.empty());
        sagaStateService.markProcessed(envelope.eventId(), envelope.eventType(), orderId);
        log.info("Saga for order {} fully compensated after refund", orderId);
    }

    /** Releases held stock and fails the order. Reads what to release from the saga record. */
    void compensateStock(SagaState saga, String reason) {
        Map<String, Object> release = new LinkedHashMap<>();
        release.put("orderId", saga.getOrderId());
        release.put("items", readJson(saga.getItems()));
        publisher.publish("stock-release", saga.getOrderId(), "stock.release.requested", release);

        publishOrderFailed(saga.getOrderId(), saga.getUserId(), reason);
        // Stock release has no result topic, so the saga is closed here rather than waiting.
        sagaStateService.transition(saga.getOrderId(), SagaStatus.FAILED_COMPENSATED, SagaContext.empty());
    }

    /** Refunds the payment and releases stock, then fails the order. */
    void compensateShipment(SagaState saga, String reason) {
        Map<String, Object> refund = new LinkedHashMap<>();
        refund.put("orderId", saga.getOrderId());
        refund.put("amount", saga.getAmount());
        publisher.publish("payment-refund", saga.getOrderId(), "payment.refund.requested", refund);

        // Goods were never shipped, so the hold must come back too - the previous code
        // refunded the money but left the stock reserved.
        Map<String, Object> release = new LinkedHashMap<>();
        release.put("orderId", saga.getOrderId());
        release.put("items", readJson(saga.getItems()));
        publisher.publish("stock-release", saga.getOrderId(), "stock.release.requested", release);

        publishOrderFailed(saga.getOrderId(), saga.getUserId(), reason);
    }

    private void publishOrderFailed(String orderId, String userId, String reason) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("orderId", orderId);
        // Carried so notification-service can actually reach the customer; order-failed
        // previously had no userId, so every such notification was skipped.
        event.put("userId", userId);
        event.put("status", "FAILED");
        event.put("reason", reason);
        publisher.publish("order-failed", orderId, "order.failed", event);
    }

    private boolean skip(Envelope envelope, String orderId) {
        if (sagaStateService.alreadyProcessed(envelope.eventId())) {
            log.debug("Skipping already-processed event {} for order {}", envelope.eventId(), orderId);
            return true;
        }
        return false;
    }

    private Envelope envelope(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        JsonNode payload = root.get("payload");
        return new Envelope(
                text(root, "eventId"),
                text(root, "eventType"),
                payload == null || payload.isNull() ? root : payload);
    }

    private String writeJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.toString();
    }

    private Object readJson(String json) {
        if (json == null || json.isBlank()) {
            return java.util.List.of();
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("Stored saga items are unreadable: {}", json, e);
            return java.util.List.of();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String required(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Saga event is missing " + field);
        }
        return value;
    }

    private record Envelope(String eventId, String eventType, JsonNode payload) {}
}
