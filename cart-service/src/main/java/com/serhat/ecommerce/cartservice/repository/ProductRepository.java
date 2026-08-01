package com.serhat.ecommerce.cartservice.repository;

import com.serhat.ecommerce.cartservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}

