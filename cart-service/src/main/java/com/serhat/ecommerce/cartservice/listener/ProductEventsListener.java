package com.serhat.ecommerce.cartservice.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.cartservice.model.Product;
import com.serhat.ecommerce.cartservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Maintains the local catalog read-model that the cart prices against.
 *
 * <p>Events arrive wrapped in the shared envelope, so the payload is read from
 * {@code payload} rather than from the message root. Only the handful of fields the cart
 * actually needs are projected - the cart has no use for descriptions, images or variants,
 * and copying them would couple it to catalog changes it does not care about.
 *
 * <p>Exceptions are left to propagate so the shared Kafka error handler retries and then
 * dead-letters, instead of a malformed message being silently dropped.
 */
@Slf4j
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
        JsonNode payload = payloadOf(message);
        JsonNode id = payload.get("productId");
        if (id != null && !id.isNull()) {
            productRepository.deleteById(id.asLong());
        }
    }

    private void upsert(String message) throws Exception {
        JsonNode payload = payloadOf(message);

        Long id = payload.path("id").asLong();
        if (id == 0) {
            throw new IllegalArgumentException("Product event carries no id: " + message);
        }

        // A withdrawn product is dropped from the read-model so it can no longer be added
        // to a cart, while carts that already contain it fail loudly at checkout.
        String status = payload.path("status").asText(null);
        if ("INACTIVE".equals(status)) {
            productRepository.deleteById(id);
            return;
        }

        Product product = new Product(id, payload.path("name").asText(null),
                new BigDecimal(payload.path("price").asText("0")));
        productRepository.save(product);
    }

    private JsonNode payloadOf(String message) throws Exception {
        JsonNode root = mapper.readTree(message);
        JsonNode payload = root.get("payload");
        // Tolerates an unwrapped message so an event published before the envelope was
        // introduced still applies rather than poisoning the consumer.
        return payload == null || payload.isNull() ? root : payload;
    }
}
