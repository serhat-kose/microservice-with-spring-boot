package com.example.cartservice.controller;

import com.example.cartservice.dto.CartDtos;
import com.example.cartservice.model.Cart;
import com.example.cartservice.model.CartItem;
import com.example.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/{userId}")
    public ResponseEntity<Cart> getCart(@PathVariable String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/{userId}/items")
    public ResponseEntity<Cart> addItem(@PathVariable String userId,
                                        @RequestBody CartDtos.AddItemRequest req) {
        CartItem item = new CartItem(req.getProductId(), req.getQuantity(), req.getPrice());
        return ResponseEntity.ok(cartService.addItem(userId, item));
    }

    @DeleteMapping("/{userId}/items/{productId}")
    public ResponseEntity<Cart> removeItem(@PathVariable String userId,
                                           @PathVariable String productId) {
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    @PostMapping("/{userId}/checkout")
    public ResponseEntity<Void> checkout(@PathVariable String userId) {
        cartService.checkout(userId);
        return ResponseEntity.accepted().build();
    }
}

