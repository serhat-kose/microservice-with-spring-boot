package com.serhat.ecommerce.stockservice.repository;

import com.serhat.ecommerce.stockservice.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductId(String productId);
    boolean existsByProductId(String productId);
    void deleteByProductId(String productId);
}
