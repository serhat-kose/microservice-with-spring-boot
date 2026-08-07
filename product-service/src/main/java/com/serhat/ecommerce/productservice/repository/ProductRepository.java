package com.serhat.ecommerce.productservice.repository;

import com.serhat.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs the filtered listing endpoint, so category/brand/
 * price/keyword filters compose into a single query instead of being applied in memory.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Detail lookup pulls images and variants in the same query. Without the entity graph
     * rendering a product page issues one extra query per collection (the classic N+1 on a
     * page that is hit on every product view).
     */
    @EntityGraph(attributePaths = {"images", "variants", "brand", "category", "seller"})
    Optional<Product> findWithDetailsBySlug(String slug);

    @EntityGraph(attributePaths = {"images", "variants", "brand", "category", "seller"})
    Optional<Product> findWithDetailsById(Long id);

    boolean existsBySlug(String slug);
}
