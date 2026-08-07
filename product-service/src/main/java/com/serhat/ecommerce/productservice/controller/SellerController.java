package com.serhat.ecommerce.productservice.controller;

import com.serhat.ecommerce.productservice.dto.CatalogDtos.SellerRequest;
import com.serhat.ecommerce.productservice.dto.CatalogDtos.SellerResponse;
import com.serhat.ecommerce.productservice.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @GetMapping("/slug/{slug}")
    public ResponseEntity<SellerResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(sellerService.getBySlug(slug));
    }

    /** Onboarding a merchant is an operator action, not something a seller does for itself. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerResponse> create(@Valid @RequestBody SellerRequest request) {
        return new ResponseEntity<>(sellerService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerResponse> update(@PathVariable Long id, @Valid @RequestBody SellerRequest request) {
        return ResponseEntity.ok(sellerService.update(id, request));
    }
}
