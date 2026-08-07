package com.serhat.ecommerce.productservice.dto;

import com.serhat.ecommerce.productservice.entity.Product;
import com.serhat.ecommerce.productservice.entity.ProductImage;
import com.serhat.ecommerce.productservice.entity.ProductStatus;
import com.serhat.ecommerce.productservice.entity.ProductVariant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ProductDtos {

    public record ImageRequest(
            @NotBlank @Size(max = 1000) String url,
            String altText,
            Integer displayOrder
    ) {}

    public record VariantRequest(
            @NotBlank String sku,
            BigDecimal price,
            BigDecimal listPrice,
            Map<String, String> attributes
    ) {}

    public record ProductRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String slug,
            @Size(max = 4000) String description,
            @NotNull @DecimalMin(value = "0.0") BigDecimal basePrice,
            Long categoryId,
            Long brandId,
            Long sellerId,
            ProductStatus status,
            @Valid List<ImageRequest> images,
            @Valid List<VariantRequest> variants
    ) {}

    public record ImageResponse(Long id, String url, String altText, Integer displayOrder) {
        public static ImageResponse from(ProductImage image) {
            return new ImageResponse(image.getId(), image.getUrl(), image.getAltText(), image.getDisplayOrder());
        }
    }

    public record VariantResponse(Long id, String sku, BigDecimal price, BigDecimal listPrice,
                                  Map<String, String> attributes, boolean active) {
        public static VariantResponse from(ProductVariant variant) {
            return new VariantResponse(variant.getId(), variant.getSku(), variant.effectivePrice(),
                    variant.getListPrice(), variant.getAttributes(), variant.isActive());
        }
    }

    /** Trimmed shape for listing/grid views - no images beyond the first, no variants. */
    public record ProductSummary(
            Long id,
            String name,
            String slug,
            BigDecimal basePrice,
            String primaryImageUrl,
            String brandName,
            String categoryName,
            BigDecimal averageRating,
            Integer reviewCount
    ) {
        public static ProductSummary from(Product product) {
            String image = product.getImages().isEmpty() ? null : product.getImages().get(0).getUrl();
            return new ProductSummary(
                    product.getId(), product.getName(), product.getSlug(), product.getBasePrice(), image,
                    product.getBrand() == null ? null : product.getBrand().getName(),
                    product.getCategory() == null ? null : product.getCategory().getName(),
                    product.getAverageRating(), product.getReviewCount());
        }
    }

    /** Full shape for the product detail page. */
    public record ProductDetail(
            Long id,
            String name,
            String slug,
            String description,
            BigDecimal basePrice,
            ProductStatus status,
            Long categoryId,
            String categoryName,
            Long brandId,
            String brandName,
            Long sellerId,
            String sellerName,
            BigDecimal averageRating,
            Integer reviewCount,
            List<ImageResponse> images,
            List<VariantResponse> variants,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ProductDetail from(Product p) {
            return new ProductDetail(
                    p.getId(), p.getName(), p.getSlug(), p.getDescription(), p.getBasePrice(), p.getStatus(),
                    p.getCategory() == null ? null : p.getCategory().getId(),
                    p.getCategory() == null ? null : p.getCategory().getName(),
                    p.getBrand() == null ? null : p.getBrand().getId(),
                    p.getBrand() == null ? null : p.getBrand().getName(),
                    p.getSeller() == null ? null : p.getSeller().getId(),
                    p.getSeller() == null ? null : p.getSeller().getName(),
                    p.getAverageRating(), p.getReviewCount(),
                    p.getImages().stream().map(ImageResponse::from).toList(),
                    p.getVariants().stream().map(VariantResponse::from).toList(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    /** Envelope so clients get paging metadata rather than a bare array. */
    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
            return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.isLast());
        }
    }
}
