package com.serhat.ecommerce.cartservice.service;

import com.serhat.ecommerce.cartservice.dto.CartDtos;
import com.serhat.ecommerce.cartservice.model.Cart;
import com.serhat.ecommerce.cartservice.model.CartItem;
import com.serhat.ecommerce.cartservice.repository.CartRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
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
    public Cart removeItem(String userId, String productId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow();
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(String userId) {
        cartRepository.findByUserId(userId).ifPresent(c -> {
            c.getItems().clear();
            cartRepository.save(c);
        });
    }

    @Transactional
    public void checkout(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow();
        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartDtos.CheckoutEvent event = new CartDtos.CheckoutEvent(userId, cart.getItems(), total);
        kafkaTemplate.send("cart-checkout", event);
        // temizle (saga başarısına bırakılmak istenirse bu satır saga yerine orchestrator'da yapılabilir)
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}

