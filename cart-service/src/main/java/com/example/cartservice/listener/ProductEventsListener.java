package com.example.cartservice.listener;

import com.example.cartservice.entity.Product;
import com.example.cartservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProductEventsListener {

    private final ProductRepository productRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ProductEventsListener(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @KafkaListener(topics = "product-created", groupId = "cart-group")
    public void onProductCreated(String message) throws Exception {
        // expect product JSON
        Product p = mapper.readValue(message, Product.class);
        productRepository.save(p);
    }

    @KafkaListener(topics = "product-updated", groupId = "cart-group")
    public void onProductUpdated(String message) throws Exception {
        Product p = mapper.readValue(message, Product.class);
        productRepository.save(p);
    }

    @KafkaListener(topics = "product-deleted", groupId = "cart-group")
    public void onProductDeleted(String message) throws Exception {
        Map<?,?> m = mapper.readValue(message, Map.class);
        Object idObj = m.get("productId");
        Long id = null;
        if (idObj instanceof Number) id = ((Number) idObj).longValue();
        else if (idObj instanceof String) id = Long.valueOf((String) idObj);
        if (id != null) productRepository.deleteById(id);
    }
}

