package com.serhat.ecommerce.productservice.event;

import com.serhat.ecommerce.productservice.entity.Product;
import com.serhat.ecommerce.productservice.entity.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * The denormalised product shape published to Kafka.
 *
 * <p>Deliberately not the JPA entity: consumers (cart-service's read-model, and
 * search-service's index) must not be coupled to product-service's persistence mapping,
 * and serialising the entity would drag lazy associations into the payload.
 */
public record ProductEventPayload(
        Long id,
        String name,
        String slug,
        String description,
        BigDecimal price,
        ProductStatus status,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        Long sellerId,
        String sellerName,
        BigDecimal averageRating,
        Integer reviewCount,
        List<String> imageUrls,
        List<VariantPayload> variants
) {

    public record VariantPayload(String sku, BigDecimal price, Map<String, String> attributes, boolean active) {}

    public static ProductEventPayload from(Product p) {
        return new ProductEventPayload(
                p.getId(), p.getName(), p.getSlug(), p.getDescription(), p.getBasePrice(), p.getStatus(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? null : p.getCategory().getName(),
                p.getBrand() == null ? null : p.getBrand().getId(),
                p.getBrand() == null ? null : p.getBrand().getName(),
                p.getSeller() == null ? null : p.getSeller().getId(),
                p.getSeller() == null ? null : p.getSeller().getName(),
                p.getAverageRating(), p.getReviewCount(),
                p.getImages().stream().map(image -> image.getUrl()).toList(),
                p.getVariants().stream()
                        .map(v -> new VariantPayload(v.getSku(), v.effectivePrice(), v.getAttributes(), v.isActive()))
                        .toList());
    }
}
