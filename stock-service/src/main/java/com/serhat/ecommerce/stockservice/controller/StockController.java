package com.serhat.ecommerce.stockservice.controller;

import com.serhat.ecommerce.stockservice.model.Stock;
import com.serhat.ecommerce.stockservice.service.StockService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockService svc;

    public StockController(StockService svc) {
        this.svc = svc;
    }

    // DTOs
    public static record CreateStockRequest(@NotBlank String productId, @Min(0) int quantity) {}
    public static record UpdateStockRequest(@Min(0) int quantity) {}

    @PostMapping
    public ResponseEntity<Stock> create(@Valid @RequestBody CreateStockRequest req) {
        try {
            Stock created = svc.createStock(req.productId(), req.quantity());
            return ResponseEntity.created(URI.create("/api/stocks/" + created.getProductId())).body(created);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Stock>> listAll() {
        return ResponseEntity.ok(svc.listAll());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Stock> getByProduct(@PathVariable String productId) {
        return svc.getByProductId(productId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{productId}/available")
    public ResponseEntity<Void> checkAvailability(@PathVariable String productId, @RequestParam(name = "quantity", defaultValue = "1") int quantity) {
        boolean ok = svc.isAvailable(productId, quantity);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).build();
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Stock> updateQuantity(@PathVariable String productId, @Valid @RequestBody UpdateStockRequest req) {
        try {
            Stock updated = svc.updateQuantity(productId, req.quantity());
            return ResponseEntity.ok(updated);
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
