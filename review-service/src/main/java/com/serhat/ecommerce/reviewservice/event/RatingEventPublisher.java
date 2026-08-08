package com.serhat.ecommerce.reviewservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import com.serhat.ecommerce.commons.web.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Publishes a product's recomputed rating so product-service and search-service can update
 * their denormalised copies.
 *
 * <p>Sent after the review transaction commits, so a rolled-back review never announces a
 * rating that was not persisted. A lost publish here is self-correcting: the next review on
 * the same product republishes the full recomputed figures rather than a delta, so the
 * copies converge instead of drifting permanently.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RatingEventPublisher {

    public static final String TOPIC = "product-rating-updated";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    public void publishRatingUpdated(Long productId, double averageRating, long reviewCount) {
        applicationEventPublisher.publishEvent(new RatingUpdated(productId, averageRating, reviewCount));
    }

    @TransactionalEventListener
    public void onCommitted(RatingUpdated event) {
        try {
            EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                    "product.rating.updated",
                    MDC.get(CorrelationIdFilter.MDC_KEY),
                    Map.of("productId", event.productId(),
                            "averageRating", event.averageRating(),
                            "reviewCount", event.reviewCount()));

            kafkaTemplate.send(TOPIC, String.valueOf(event.productId()),
                    objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.error("Failed to publish rating update for product {}", event.productId(), e);
        }
    }

    public record RatingUpdated(Long productId, double averageRating, long reviewCount) {}
}
