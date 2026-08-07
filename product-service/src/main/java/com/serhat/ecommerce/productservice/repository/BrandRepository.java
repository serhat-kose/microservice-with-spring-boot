package com.serhat.ecommerce.productservice.repository;

import com.serhat.ecommerce.productservice.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Brand> findByActiveTrue(Pageable pageable);
}
