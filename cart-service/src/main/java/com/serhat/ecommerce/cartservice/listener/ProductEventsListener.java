package com.serhat.ecommerce.cartservice.listener;

import com.serhat.ecommerce.cartservice.model.Product;
import com.serhat.ecommerce.cartservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ProductEventsListener {

    private final ProductRepository productRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProductEventsListener(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @KafkaListener(topics = "product-created", groupId = "cart-group")
    public void onProductCreated(String message) {
        handle(message, this::upsert);
    }

    @KafkaListener(topics = "product-updated", groupId = "cart-group")
    public void onProductUpdated(String message) {
        handle(message, this::upsert);
    }

    @KafkaListener(topics = "product-deleted", groupId = "cart-group")
    public void onProductDeleted(String message) {
        handle(message, this::delete);
    }

    private void upsert(String message) throws Exception {
        Product p = mapper.readValue(message, Product.class);
        productRepository.save(p);
    }

    private void delete(String message) throws Exception {
        Map<?, ?> m = mapper.readValue(message, Map.class);
        Object idObj = m.get("productId");
        Long id = null;
        if (idObj instanceof Number number) id = number.longValue();
        else if (idObj instanceof String s) id = Long.valueOf(s);
        if (id != null) productRepository.deleteById(id);
    }

    @FunctionalInterface
    private interface MessageHandler {
        void handle(String message) throws Exception;
    }

    private void handle(String message, MessageHandler handler) {
        try {
            handler.handle(message);
        } catch (Exception e) {
            log.error("Failed to process product event message: {}", message, e);
        }
    }
}

