package com.serhat.ecommerce.productservice.repository;

import com.serhat.ecommerce.productservice.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    /**
     * Writes only the rating columns, deliberately leaving {@code version} untouched.
     * Loading and saving the entity instead would bump the optimistic-lock version and make
     * an unrelated, concurrent seller edit fail for no real reason.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.averageRating = :average, p.reviewCount = :count WHERE p.id = :id")
    int updateRating(@Param("id") Long id, @Param("average") BigDecimal average, @Param("count") int count);
}
