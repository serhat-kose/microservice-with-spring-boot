package com.serhat.ecommerce.orderservice.repository;

import com.serhat.ecommerce.orderservice.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Paged rather than returning everything: a long-standing customer's history is
     * unbounded, and the previous {@code List findByUserId} loaded all of it on every call.
     */
    Page<Order> findByUserId(String userId, Pageable pageable);

    /** Fetches lines with the order so rendering a detail page is a single query. */
    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(Long id);
}
