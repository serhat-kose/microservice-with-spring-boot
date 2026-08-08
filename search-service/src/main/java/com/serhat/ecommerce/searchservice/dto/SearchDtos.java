package com.serhat.ecommerce.searchservice.dto;

import com.serhat.ecommerce.searchservice.document.ProductDocument;

import java.math.BigDecimal;
import java.util.List;

public class SearchDtos {

    /** One product card in a results grid. */
    public record SearchHitDto(
            Long id,
            String name,
            String slug,
            BigDecimal price,
            String primaryImageUrl,
            String brandName,
            String categoryName,
            BigDecimal averageRating,
            Integer reviewCount,
            Double score
    ) {
        public static SearchHitDto from(ProductDocument doc, Double score) {
            return new SearchHitDto(Long.valueOf(doc.getId()), doc.getName(), doc.getSlug(),
                    doc.getPrice(), doc.getPrimaryImageUrl(), doc.getBrandName(), doc.getCategoryName(),
                    doc.getAverageRating(), doc.getReviewCount(), score);
        }
    }

    /** A single selectable value in a facet, with how many results it would leave. */
    public record FacetValue(String value, long count) {}

    public record Facet(String name, List<FacetValue> values) {}

    public record PriceRange(BigDecimal min, BigDecimal max) {}

    public record SearchResponse(
            List<SearchHitDto> results,
            long totalHits,
            int page,
            int size,
            int totalPages,
            List<Facet> facets,
            PriceRange priceRange
    ) {}
}
