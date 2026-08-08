package com.serhat.ecommerce.cartservice.controller;

import com.serhat.ecommerce.cartservice.dto.CartDtos;
import com.serhat.ecommerce.cartservice.dto.CartDtos.CartResponse;
import com.serhat.ecommerce.cartservice.service.CartService;
import com.serhat.ecommerce.cartservice.service.CartService.PricedCart;
import com.serhat.ecommerce.commons.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Operates on the <em>authenticated</em> caller's cart. The owner used to be a
 * {@code {userId}} path variable, which let any logged-in account read, mutate and check
 * out anyone else's cart simply by changing the value in the URL.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ok(cartService.getPricedCart(CurrentUser.requireUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartDtos.AddItemRequest req) {
        return ok(cartService.addItem(CurrentUser.requireUserId(), req.getProductId(), req.getQuantity()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long productId) {
        return ok(cartService.removeItem(CurrentUser.requireUserId(), productId));
    }

    @PostMapping("/coupon")
    public ResponseEntity<CartResponse> applyCoupon(@Valid @RequestBody CartDtos.ApplyCouponRequest req) {
        return ok(cartService.applyCoupon(CurrentUser.requireUserId(), req.code()));
    }

    @DeleteMapping("/coupon")
    public ResponseEntity<CartResponse> removeCoupon() {
        return ok(cartService.removeCoupon(CurrentUser.requireUserId()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<Void> checkout(@Valid @RequestBody CartDtos.CheckoutRequest req) {
        cartService.checkout(CurrentUser.requireUserId(), req.shippingAddress());
        return ResponseEntity.accepted().build();
    }

    private ResponseEntity<CartResponse> ok(PricedCart priced) {
        return ResponseEntity.ok(
                CartResponse.from(priced.cart(), priced.quote(), priced.couponMessage()));
    }
}
