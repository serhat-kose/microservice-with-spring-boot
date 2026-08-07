package com.serhat.ecommerce.productservice.repository;

import com.serhat.ecommerce.productservice.entity.Product;
import com.serhat.ecommerce.productservice.entity.ProductStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Composable filters for the catalog listing endpoint.
 *
 * <p>Note the keyword predicate is a simple {@code LIKE} on name/description. It exists so
 * the listing endpoint is usable on its own, but relevance-ranked, typo-tolerant search
 * with facets is served by search-service against Elasticsearch - this is not meant to
 * compete with it.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Shoppers must never see DRAFT or INACTIVE listings. */
    public static Specification<Product> visibleToShoppers() {
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Product> inCategories(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.join("category", JoinType.LEFT).get("id").in(categoryIds);
    }

    public static Specification<Product> hasBrand(Long brandId) {
        return brandId == null ? null
                : (root, query, cb) -> cb.equal(root.join("brand", JoinType.LEFT).get("id"), brandId);
    }

    public static Specification<Product> hasSeller(Long sellerId) {
        return sellerId == null ? null
                : (root, query, cb) -> cb.equal(root.join("seller", JoinType.LEFT).get("id"), sellerId);
    }

    public static Specification<Product> priceAtLeast(BigDecimal min) {
        return min == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("basePrice"), min);
    }

    public static Specification<Product> priceAtMost(BigDecimal max) {
        return max == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("basePrice"), max);
    }

    public static Specification<Product> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public static Specification<Product> minimumRating(BigDecimal minRating) {
        return minRating == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("averageRating"), minRating);
    }
}
