package com.serhat.ecommerce.stockservice.repository;

import com.serhat.ecommerce.stockservice.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByProductId(String productId);
    boolean existsByProductId(String productId);
    void deleteByProductId(String productId);

    /**
     * Atomically decrements quantity only if enough stock is available, avoiding the
     * check-then-act race between a separate "read quantity" and "write quantity" call.
     * Returns the number of rows updated (0 means insufficient stock or unknown product).
     */
    @Modifying
    @Query("UPDATE Stock s SET s.quantity = s.quantity - :qty WHERE s.productId = :productId AND s.quantity >= :qty")
    int decrementIfAvailable(@Param("productId") String productId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE Stock s SET s.quantity = s.quantity + :qty WHERE s.productId = :productId")
    int increment(@Param("productId") String productId, @Param("qty") int qty);
}
