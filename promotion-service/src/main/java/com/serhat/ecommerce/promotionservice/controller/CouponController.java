package com.serhat.ecommerce.promotionservice.controller;

import com.serhat.ecommerce.commons.security.CurrentUser;
import com.serhat.ecommerce.promotionservice.dto.CouponDtos.*;
import com.serhat.ecommerce.promotionservice.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class CouponController {

    private static final int MAX_PAGE_SIZE = 100;

    private final CouponService couponService;

    /**
     * Quotes a coupon for the caller's basket. Requires authentication because per-user
     * limits are part of the answer, and takes the user from the verified identity so a
     * caller cannot check someone else's remaining allowance.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@Valid @RequestBody ValidateRequest request) {
        return ResponseEntity.ok(couponService.validate(
                CurrentUser.requireUserId(), request.code(), request.orderAmount()));
    }

    @PostMapping("/redeem")
    public ResponseEntity<ValidateResponse> redeem(@Valid @RequestBody RedeemRequest request) {
        return ResponseEntity.ok(couponService.redeem(
                CurrentUser.requireUserId(), request.code(), request.orderAmount(), request.orderId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<CouponResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PageResponse.of(couponService.list(pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return new ResponseEntity<>(couponService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> update(@PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        couponService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
