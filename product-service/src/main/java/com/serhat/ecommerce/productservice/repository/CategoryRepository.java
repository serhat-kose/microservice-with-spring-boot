package com.serhat.ecommerce.productservice.repository;

import com.serhat.ecommerce.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();

    List<Category> findByParentIdAndActiveTrueOrderByDisplayOrderAsc(Long parentId);

    /**
     * Loads the whole active tree in one query. The tree is small and read on nearly every
     * page, so fetching it wholesale and assembling in memory beats a query per level.
     */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.active = true ORDER BY c.displayOrder ASC")
    List<Category> findAllActiveWithParent();
}
