package com.serhat.ecommerce.productservice.repository;

import com.serhat.ecommerce.productservice.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findBySlug(String slug);

    /** Used to resolve the storefront belonging to the authenticated SELLER account. */
    Optional<Seller> findByUserId(String userId);

    boolean existsBySlug(String slug);
}
