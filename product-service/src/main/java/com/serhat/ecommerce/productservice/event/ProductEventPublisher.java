package com.serhat.ecommerce.productservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.productservice.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private static final String TOPIC_CREATED = "product-created";
    private static final String TOPIC_UPDATED = "product-updated";
    private static final String TOPIC_DELETED = "product-deleted";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishCreated(Product product) {
        publish(TOPIC_CREATED, product);
    }

    public void publishUpdated(Product product) {
        publish(TOPIC_UPDATED, product);
    }

    public void publishDeleted(Long productId) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("productId", productId));
            kafkaTemplate.send(TOPIC_DELETED, String.valueOf(productId), json);
        } catch (Exception e) {
            log.error("Failed to publish product-deleted event for id {}", productId, e);
        }
    }

    private void publish(String topic, Product product) {
        try {
            String json = objectMapper.writeValueAsString(product);
            kafkaTemplate.send(topic, String.valueOf(product.getId()), json);
        } catch (Exception e) {
            log.error("Failed to publish {} event for product id {}", topic, product.getId(), e);
        }
    }
}
