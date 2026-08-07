package com.serhat.ecommerce.cartservice.service;

import com.serhat.ecommerce.cartservice.dto.CartDtos;
import com.serhat.ecommerce.cartservice.exception.CartNotFoundException;
import com.serhat.ecommerce.cartservice.model.Cart;
import com.serhat.ecommerce.cartservice.model.CartItem;
import com.serhat.ecommerce.cartservice.model.Product;
import com.serhat.ecommerce.cartservice.repository.CartRepository;
import com.serhat.ecommerce.cartservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> newCart(userId));
    }

    /**
     * Resolves the product and its price from the local catalog read-model - fed by
     * {@code product-created}/{@code product-updated} events - rather than trusting the
     * request. The read-model was previously written by the event listener but never read,
     * so the client's own price was what got stored and charged.
     */
    @Transactional
    public Cart addItem(String userId, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown product: " + productId));

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> newCart(userId));

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            // Re-snapshot so an existing line reflects the current catalog price too.
            item.setPrice(product.getPrice());
            item.setProductName(product.getName());
        } else {
            cart.getItems().add(new CartItem(productId, product.getName(), quantity, product.getPrice()));
        }
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(String userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new CartNotFoundException(userId));
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        return cartRepository.save(cart);
    }

    /**
     * Re-prices every line against the catalog immediately before publishing, so a price
     * change while the cart sat idle cannot be checked out at the stale price.
     *
     * <p>Waits for the Kafka send to be acknowledged before clearing the cart, so a broker
     * failure leaves the cart intact instead of silently dropping the order.
     */
    @Transactional
    public void checkout(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new CartNotFoundException(userId));
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Product is no longer available: " + item.getProductId()));
            item.setPrice(product.getPrice());
            item.setProductName(product.getName());
        }

        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartDtos.CheckoutEvent event = new CartDtos.CheckoutEvent(userId, cart.getItems(), total);
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("cart-checkout", userId, json).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to publish cart-checkout event for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Checkout is temporarily unavailable, please retry");
        }

        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart newCart(String userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        return cart;
    }
}
