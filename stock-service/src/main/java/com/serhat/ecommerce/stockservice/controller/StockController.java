package com.serhat.ecommerce.stockservice.controller;

import com.serhat.ecommerce.stockservice.dto.StockDtos.AvailabilityResponse;
import com.serhat.ecommerce.stockservice.dto.StockDtos.CreateStockRequest;
import com.serhat.ecommerce.stockservice.dto.StockDtos.StockResponse;
import com.serhat.ecommerce.stockservice.dto.StockDtos.UpdateStockRequest;
import com.serhat.ecommerce.stockservice.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Inventory administration. Every endpoint requires a merchant or operator - previously
 * any registered account could set any product's stock to zero or to an arbitrary number.
 */
@RestController
@RequestMapping("/api/stocks")
@PreAuthorize("hasAnyRole('ADMIN','SELLER')")
public class StockController {

    private final StockService svc;

    public StockController(StockService svc) {
        this.svc = svc;
    }

    @PostMapping
    public ResponseEntity<StockResponse> create(@Valid @RequestBody CreateStockRequest req) {
        try {
            StockResponse created = StockResponse.from(svc.createStock(req.productId(), req.quantity()));
            return ResponseEntity.created(URI.create("/api/stocks/" + created.productId())).body(created);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<StockResponse>> listAll() {
        return ResponseEntity.ok(svc.listAll().stream().map(StockResponse::from).toList());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> getByProduct(@PathVariable String productId) {
        return svc.getByProductId(productId)
                .map(StockResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{productId}/available")
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @PathVariable String productId,
            @RequestParam(name = "quantity", defaultValue = "1") int quantity) {
        boolean available = svc.isAvailable(productId, quantity);
        return ResponseEntity.ok(new AvailabilityResponse(productId, quantity, available));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<StockResponse> updateQuantity(@PathVariable String productId,
                                                        @Valid @RequestBody UpdateStockRequest req) {
        try {
            return ResponseEntity.ok(StockResponse.from(svc.updateQuantity(productId, req.quantity())));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable String productId) {
        try {
            svc.deleteByProductId(productId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
