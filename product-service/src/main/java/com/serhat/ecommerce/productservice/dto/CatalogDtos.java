package com.serhat.ecommerce.productservice.dto;

import com.serhat.ecommerce.productservice.entity.Brand;
import com.serhat.ecommerce.productservice.entity.Category;
import com.serhat.ecommerce.productservice.entity.Seller;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class CatalogDtos {

    public record CategoryRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String slug,
            String description,
            Long parentId,
            Integer displayOrder,
            Boolean active
    ) {}

    /** Flat shape used when a caller wants a single category rather than the tree. */
    public record CategoryResponse(Long id, String name, String slug, String description,
                                   Long parentId, Integer displayOrder, boolean active) {
        public static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(),
                    c.getParent() == null ? null : c.getParent().getId(),
                    c.getDisplayOrder(), c.isActive());
        }
    }

    /** Recursive shape for rendering the storefront's category navigation in one call. */
    public record CategoryNode(Long id, String name, String slug, Integer displayOrder,
                               List<CategoryNode> children) {
        public static CategoryNode of(Category c) {
            return new CategoryNode(c.getId(), c.getName(), c.getSlug(), c.getDisplayOrder(),
                    new ArrayList<>());
        }
    }

    public record BrandRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String slug,
            String logoUrl,
            Boolean active
    ) {}

    public record BrandResponse(Long id, String name, String slug, String logoUrl, boolean active) {
        public static BrandResponse from(Brand b) {
            return new BrandResponse(b.getId(), b.getName(), b.getSlug(), b.getLogoUrl(), b.isActive());
        }
    }

    public record SellerRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 255) String slug,
            @NotBlank String userId,
            Boolean active
    ) {}

    public record SellerResponse(Long id, String name, String slug, String userId, boolean active) {
        public static SellerResponse from(Seller s) {
            return new SellerResponse(s.getId(), s.getName(), s.getSlug(), s.getUserId(), s.isActive());
        }
    }
}
