package com.serhat.ecommerce.stockservice.service;

import com.serhat.ecommerce.stockservice.exception.InsufficientStockException;
import com.serhat.ecommerce.stockservice.model.Stock;
import com.serhat.ecommerce.stockservice.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class StockService {
    private final StockRepository repo;

    public StockService(StockRepository repo) {
        this.repo = repo;
    }

    /**
     * Reserves every item atomically per-row (see {@link StockRepository#decrementIfAvailable}).
     * If any item is unavailable, the whole method throws and the surrounding
     * @Transactional rolls back the decrements already applied to earlier items
     * in this same reservation.
     */
    public void reserveItems(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            String productId = (String) item.get("productId");
            int qty = ((Number) item.get("quantity")).intValue();
            int updated = repo.decrementIfAvailable(productId, qty);
            if (updated == 0) {
                throw new InsufficientStockException(productId);
            }
        }
    }

    public void releaseItems(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            String productId = (String) item.get("productId");
            int qty = ((Number) item.get("quantity")).intValue();
            int updated = repo.increment(productId, qty);
            if (updated == 0) {
                // product row no longer exists (deleted) - recreate it rather than losing the released stock
                repo.save(Stock.builder().productId(productId).quantity(qty).build());
            }
        }
    }

    public Stock createStock(String productId, int quantity) {
        if (repo.existsByProductId(productId)) {
            throw new IllegalArgumentException("Product already exists: " + productId);
        }
        Stock stock = Stock.builder()
                .productId(productId)
                .quantity(quantity)
                .build();
        return repo.save(stock);
    }

    public Optional<Stock> getByProductId(String productId) {
        return repo.findByProductId(productId);
    }

    public List<Stock> listAll() {
        return repo.findAll();
    }

    public boolean isAvailable(String productId, int requiredQuantity) {
        return repo.findByProductId(productId)
                .map(s -> s.getQuantity() != null && s.getQuantity() >= requiredQuantity)
                .orElse(false);
    }

    public Stock updateQuantity(String productId, int newQuantity) {
        Stock s = repo.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        s.setQuantity(newQuantity);
        return repo.save(s);
    }

    public void deleteByProductId(String productId) {
        if (!repo.existsByProductId(productId)) {
            throw new NoSuchElementException("Product not found: " + productId);
        }
        repo.deleteByProductId(productId);
    }
}
