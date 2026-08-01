package com.serhat.ecommerce.stockservice.service;

import com.serhat.ecommerce.stockservice.exception.InsufficientStockException;
import com.serhat.ecommerce.stockservice.model.ProcessedReservation;
import com.serhat.ecommerce.stockservice.model.Stock;
import com.serhat.ecommerce.stockservice.repository.ProcessedReservationRepository;
import com.serhat.ecommerce.stockservice.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class StockService {
    private final StockRepository repo;
    private final ProcessedReservationRepository processedReservationRepository;

    public StockService(StockRepository repo, ProcessedReservationRepository processedReservationRepository) {
        this.repo = repo;
        this.processedReservationRepository = processedReservationRepository;
    }

    /**
     * Idempotent entry point for the saga's stock-reserve-request: if this orderId was
     * already reserved (a redelivered Kafka message under at-least-once semantics),
     * skips straight to true without decrementing stock again. Otherwise reserves and
     * records the orderId as processed in the same transaction as the decrements, so a
     * crash between "decrement" and "mark processed" can't happen.
     *
     * @return true if reserved (either just now or previously); throws
     *         InsufficientStockException if this is a new reservation attempt that fails.
     */
    public boolean reserveForOrder(String orderId, List<Map<String, Object>> items) {
        if (processedReservationRepository.existsById(orderId)) {
            return true;
        }
        reserveItems(items);
        processedReservationRepository.save(new ProcessedReservation(orderId, Instant.now()));
        return true;
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
