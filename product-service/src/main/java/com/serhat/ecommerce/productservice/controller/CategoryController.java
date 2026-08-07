package com.serhat.ecommerce.productservice.controller;

import com.serhat.ecommerce.productservice.dto.CatalogDtos.CategoryNode;
import com.serhat.ecommerce.productservice.dto.CatalogDtos.CategoryRequest;
import com.serhat.ecommerce.productservice.dto.CatalogDtos.CategoryResponse;
import com.serhat.ecommerce.productservice.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** The whole navigation tree in one call, so the storefront header needs a single request. */
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryNode>> tree() {
        return ResponseEntity.ok(categoryService.tree());
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getBySlug(slug));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<CategoryResponse>> children(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.children(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return new ResponseEntity<>(categoryService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
