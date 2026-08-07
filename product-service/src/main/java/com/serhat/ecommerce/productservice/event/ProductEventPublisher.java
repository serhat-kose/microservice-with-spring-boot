package com.serhat.ecommerce.productservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.commons.web.CorrelationIdFilter;
import com.serhat.ecommerce.productservice.entity.Product;
import com.serhat.ecommerce.productservice.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Records catalog events in the outbox rather than publishing to Kafka directly.
 *
 * <p>The previous fire-and-forget send happened after the database commit, so a broker
 * outage silently desynced every consumer's read-model with nothing to replay from. Writing
 * to the outbox inside the same transaction as the product change makes the two atomic: if
 * the transaction rolls back, no event is emitted; if it commits, the relay will deliver the
 * event eventually.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    public static final String TOPIC_CREATED = "product-created";
    public static final String TOPIC_UPDATED = "product-updated";
    public static final String TOPIC_DELETED = "product-deleted";

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public void publishCreated(Product product) {
        enqueue(TOPIC_CREATED, "product.created", String.valueOf(product.getId()),
                ProductEventPayload.from(product));
    }

    public void publishUpdated(Product product) {
        enqueue(TOPIC_UPDATED, "product.updated", String.valueOf(product.getId()),
                ProductEventPayload.from(product));
    }

    public void publishDeleted(Long productId) {
        enqueue(TOPIC_DELETED, "product.deleted", String.valueOf(productId),
                Map.of("productId", productId));
    }

    private void enqueue(String topic, String eventType, String key, Object payload) {
        try {
            EventEnvelope<Object> envelope = EventEnvelope.of(eventType, MDC.get(CorrelationIdFilter.MDC_KEY), payload);
            outboxService.enqueue(topic, key, objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            // Rethrown rather than swallowed: the enclosing transaction must roll back so the
            // catalog change and its event stay consistent.
            throw new IllegalStateException("Failed to record " + eventType + " in the outbox", e);
        }
    }
}
