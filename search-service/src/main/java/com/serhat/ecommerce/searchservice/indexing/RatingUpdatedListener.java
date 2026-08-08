package com.serhat.ecommerce.searchservice.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.searchservice.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Keeps the indexed rating in step so results can be sorted and filtered by rating.
 *
 * <p>Uses a partial update rather than reindexing the document: a full rebuild here would
 * need the whole product payload, which this event does not carry, and would race with
 * catalog updates. The partial update also avoids touching the external version that
 * {@link ProductIndexer} relies on for ordering catalog changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RatingUpdatedListener {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product-rating-updated", groupId = "search-group")
    public void onRatingUpdated(String message) throws Exception {
        JsonNode payload = payloadOf(objectMapper.readTree(message));

        JsonNode productId = payload.get("productId");
        if (productId == null || productId.isNull()) {
            throw new IllegalArgumentException("Rating event carries no productId: " + message);
        }

        UpdateQuery update = UpdateQuery.builder(String.valueOf(productId.asLong()))
                .withDocument(Document.from(Map.of(
                        "averageRating", payload.path("averageRating").asDouble(0.0),
                        "reviewCount", payload.path("reviewCount").asInt(0))))
                // The product may not be indexed (draft or withdrawn); skip rather than fail.
                .withDocAsUpsert(false)
                .build();

        try {
            elasticsearchOperations.update(update, elasticsearchOperations.getIndexCoordinatesFor(ProductDocument.class));
        } catch (Exception e) {
            log.debug("Rating update skipped for product {}: {}", productId.asLong(), e.getMessage());
        }
    }

    private JsonNode payloadOf(JsonNode root) {
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }
}
