package com.serhat.ecommerce.productservice.service;

import com.serhat.ecommerce.productservice.dto.CatalogDtos.CategoryNode;
import com.serhat.ecommerce.productservice.dto.CatalogDtos.CategoryRequest;
import com.serhat.ecommerce.productservice.dto.CatalogDtos.CategoryResponse;
import com.serhat.ecommerce.productservice.entity.Category;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.CategoryCycleException;
import com.serhat.ecommerce.productservice.exception.CatalogExceptions.CategoryNotFoundException;
import com.serhat.ecommerce.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * The navigation tree is requested on virtually every storefront page but changes rarely,
     * so it is cached wholesale and evicted on any write.
     */
    @Cacheable(value = "categoryTree", key = "'tree'")
    @Transactional(readOnly = true)
    public List<CategoryNode> tree() {
        List<Category> all = categoryRepository.findAllActiveWithParent();

        Map<Long, CategoryNode> nodes = new LinkedHashMap<>();
        all.forEach(c -> nodes.put(c.getId(), CategoryNode.of(c)));

        List<CategoryNode> roots = new ArrayList<>();
        for (Category category : all) {
            CategoryNode node = nodes.get(category.getId());
            Long parentId = category.getParent() == null ? null : category.getParent().getId();
            // A child whose parent is inactive is promoted to a root rather than being
            // dropped, so deactivating a parent never hides its still-active children.
            CategoryNode parentNode = parentId == null ? null : nodes.get(parentId);
            if (parentNode == null) {
                roots.add(node);
            } else {
                parentNode.children().add(node);
            }
        }
        return roots;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new CategoryNotFoundException(slug));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> children(Long parentId) {
        return categoryRepository.findByParentIdAndActiveTrueOrderByDisplayOrderAsc(parentId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * Collects a category and every descendant, so filtering a listing by "Electronics"
     * also returns products filed under "Phones" beneath it.
     */
    @Transactional(readOnly = true)
    public Set<Long> selfAndDescendantIds(Long categoryId) {
        Set<Long> result = new HashSet<>();
        collectDescendants(categoryId, result);
        return result;
    }

    private void collectDescendants(Long categoryId, Set<Long> accumulator) {
        if (categoryId == null || !accumulator.add(categoryId)) {
            return;
        }
        categoryRepository.findByParentIdAndActiveTrueOrderByDisplayOrderAsc(categoryId)
                .forEach(child -> collectDescendants(child.getId(), accumulator));
    }

    @Transactional
    @CacheEvict(value = "categoryTree", allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        Category category = Category.builder()
                .name(request.name())
                .slug(resolveSlug(request.slug(), request.name()))
                .description(request.description())
                .displayOrder(request.displayOrder() == null ? 0 : request.displayOrder())
                .active(request.active() == null || request.active())
                .build();

        if (request.parentId() != null) {
            category.setParent(categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.parentId())));
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categoryTree", allEntries = true)
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));

        category.setName(request.name());
        if (StringUtils.hasText(request.slug())) {
            category.setSlug(SlugGenerator.slugify(request.slug()));
        }
        category.setDescription(request.description());
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }

        if (request.parentId() == null) {
            category.setParent(null);
        } else {
            if (createsCycle(id, request.parentId())) {
                throw new CategoryCycleException(id);
            }
            category.setParent(categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.parentId())));
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "categoryTree", allEntries = true)
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        categoryRepository.delete(category);
    }

    /**
     * Walking up from the proposed parent, a cycle exists if we meet the category being
     * edited. Without this check, setting a category's parent to its own child detaches
     * that whole branch from the tree and makes {@link #tree()} lose it.
     */
    private boolean createsCycle(Long categoryId, Long proposedParentId) {
        Long cursor = proposedParentId;
        Set<Long> seen = new HashSet<>();
        while (cursor != null && seen.add(cursor)) {
            if (cursor.equals(categoryId)) {
                return true;
            }
            cursor = categoryRepository.findById(cursor)
                    .map(c -> c.getParent() == null ? null : c.getParent().getId())
                    .orElse(null);
        }
        return false;
    }

    private String resolveSlug(String requested, String name) {
        String desired = StringUtils.hasText(requested) ? requested : name;
        return SlugGenerator.uniqueSlug(desired, categoryRepository::existsBySlug);
    }
}
