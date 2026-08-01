package com.serhat.ecommerce.stockservice.service;

import com.serhat.ecommerce.stockservice.model.Stock;
import com.serhat.ecommerce.stockservice.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class StockService {
    private final StockRepository repo;

    public StockService(StockRepository repo) {
        this.repo = repo;
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
