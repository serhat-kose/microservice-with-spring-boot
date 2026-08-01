package com.serhat.ecommerce.cartservice.listener;

import com.serhat.ecommerce.cartservice.model.Product;
import com.serhat.ecommerce.cartservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Exceptions are intentionally left to propagate: the container-level
 * KafkaErrorHandlingConfig retries with backoff and, on repeated failure,
 * routes the message to a dead-letter topic instead of it being silently dropped.
 */
@Component
public class ProductEventsListener {

    private final ProductRepository productRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProductEventsListener(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @KafkaListener(topics = "product-created", groupId = "cart-group")
    public void onProductCreated(String message) throws Exception {
        upsert(message);
    }

    @KafkaListener(topics = "product-updated", groupId = "cart-group")
    public void onProductUpdated(String message) throws Exception {
        upsert(message);
    }

    @KafkaListener(topics = "product-deleted", groupId = "cart-group")
    public void onProductDeleted(String message) throws Exception {
        Map<?, ?> m = mapper.readValue(message, Map.class);
        Object idObj = m.get("productId");
        Long id = null;
        if (idObj instanceof Number number) id = number.longValue();
        else if (idObj instanceof String s) id = Long.valueOf(s);
        if (id != null) productRepository.deleteById(id);
    }

    private void upsert(String message) throws Exception {
        Product p = mapper.readValue(message, Product.class);
        productRepository.save(p);
    }
}
