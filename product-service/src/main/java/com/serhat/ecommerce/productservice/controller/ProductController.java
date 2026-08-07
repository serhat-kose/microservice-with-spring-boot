package com.serhat.ecommerce.productservice.controller;

import com.serhat.ecommerce.productservice.dto.ProductDtos.PageResponse;
import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductDetail;
import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductRequest;
import com.serhat.ecommerce.productservice.dto.ProductDtos.ProductSummary;
import com.serhat.ecommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Reads are open to anonymous shoppers; writes require a merchant or operator.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductService productService;

    /**
     * Paged and filtered catalog listing. Page size is capped so a caller cannot request the
     * whole catalog in one response - the endpoint this replaces had no bound at all.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductSummary>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = Sort.by("asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);

        Page<ProductSummary> result = productService.search(
                q, categoryId, brandId, sellerId, minPrice, maxPrice, minRating, pageable);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /** Storefront links are slug-based, so the detail page resolves without exposing ids. */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductDetail> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(productService.getBySlug(slug));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<ProductDetail> create(@Valid @RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @productService.isOwnedBySellerAccount(#id, authentication.principal.userId())")
    public ResponseEntity<ProductDetail> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @productService.isOwnedBySellerAccount(#id, authentication.principal.userId())")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
