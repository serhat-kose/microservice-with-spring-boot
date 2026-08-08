package com.serhat.ecommerce.productservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Applies rating figures recomputed by review-service to the denormalised copy on Product,
 * so listings can sort and filter by rating without a cross-service call.
 *
 * <p>Updated through a targeted query rather than by loading and saving the entity: a normal
 * save would bump the {@code @Version} column and make a concurrent seller edit fail with a
 * spurious optimistic-lock conflict, even though the two changes touch different fields.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RatingUpdatedListener {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "product-rating-updated", groupId = "product-group")
    @Transactional
    @CacheEvict(value = "productDetail", allEntries = true)
    public void onRatingUpdated(String message) throws Exception {
        JsonNode payload = payloadOf(objectMapper.readTree(message));

        JsonNode productId = payload.get("productId");
        if (productId == null || productId.isNull()) {
            throw new IllegalArgumentException("Rating event carries no productId: " + message);
        }

        BigDecimal average = new BigDecimal(payload.path("averageRating").asText("0"));
        int count = payload.path("reviewCount").asInt(0);

        int updated = productRepository.updateRating(productId.asLong(), average, count);
        if (updated == 0) {
            // The product may have been withdrawn since the review was written; nothing to do.
            log.debug("Rating update for unknown product {} ignored", productId.asLong());
        }
    }

    private JsonNode payloadOf(JsonNode root) {
        JsonNode payload = root.get("payload");
        return payload == null || payload.isNull() ? root : payload;
    }
}
