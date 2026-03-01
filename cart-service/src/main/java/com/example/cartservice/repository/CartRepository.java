package com.example.cartservice.repository;

import com.example.cartservice.model.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(String userId);
    void deleteByUserId(String userId);
}

