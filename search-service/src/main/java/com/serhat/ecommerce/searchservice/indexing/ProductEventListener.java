package com.serhat.ecommerce.searchservice.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Consumes catalog events and keeps the index in step with product-service.
 *
 * <p>Exceptions propagate to the shared Kafka error handler so a failure is retried and then
 * dead-lettered. Swallowing them here would let the index drift out of sync with the catalog
 * with nothing recording that it happened.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final ProductIndexer indexer;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"product-created", "product-updated"}, groupId = "search-group")
    public void onProductChanged(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        indexer.index(payloadOf(root), occurredAt(root));
    }

    @KafkaListener(topics = "product-deleted", groupId = "search-group")
    public void onProductDeleted(String message) throws Exception {
        JsonNode payload = payloadOf(objectMapper.readTree(message));
        JsonNode id = payload.get("productId");
        if (id != null && !id.isNull()) {
            indexer.delete(id.asLong());
        }
    }

    private JsonNode payloadOf(JsonNode root) {
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }

    /**
     * The envelope timestamp doubles as the document version, so it must be stable across
     * redeliveries of the same event - falling back to "now" is only for events published
     * before the envelope existed.
     */
    private Instant occurredAt(JsonNode root) {
        JsonNode value = root.get("occurredAt");
        if (value == null || value.isNull()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value.asText());
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
