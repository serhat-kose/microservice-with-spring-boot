package com.serhat.ecommerce.cartservice.controller;

import com.serhat.ecommerce.cartservice.dto.CartDtos;
import com.serhat.ecommerce.cartservice.dto.CartDtos.CartResponse;
import com.serhat.ecommerce.cartservice.model.CartItem;
import com.serhat.ecommerce.cartservice.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(CartResponse.from(cartService.getCart(userId)));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<CartResponse> addItem(@PathVariable String userId,
                                        @Valid @RequestBody CartDtos.AddItemRequest req) {
        CartItem item = new CartItem(req.getProductId(), req.getQuantity(), req.getPrice());
        return ResponseEntity.ok(CartResponse.from(cartService.addItem(userId, item)));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable String userId,
                                           @PathVariable Long productId) {
        return ResponseEntity.ok(CartResponse.from(cartService.removeItem(userId, productId)));
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<Void> checkout(@PathVariable String userId) {
        cartService.checkout(userId);
        return ResponseEntity.accepted().build();
    }
}
