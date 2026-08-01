package com.serhat.ecommerce.cartservice.service;

import com.serhat.ecommerce.cartservice.dto.CartDtos;
import com.serhat.ecommerce.cartservice.exception.CartNotFoundException;
import com.serhat.ecommerce.cartservice.model.Cart;
import com.serhat.ecommerce.cartservice.model.CartItem;
import com.serhat.ecommerce.cartservice.repository.CartRepository;
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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public Cart getCart(String userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            return c;
        });
    }

    @Transactional
    public Cart addItem(String userId, CartItem item) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            return c;
        });

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(item.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + item.getQuantity());
        } else {
            cart.getItems().add(item);
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
     * Waits for the Kafka send to be acknowledged before clearing the cart, so a
     * broker failure leaves the cart intact instead of silently dropping the order
     * (previously the cart was cleared unconditionally right after a fire-and-forget send).
     */
    @Transactional
    public void checkout(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new CartNotFoundException(userId));
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
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
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Checkout is temporarily unavailable, please retry");
        }

        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
