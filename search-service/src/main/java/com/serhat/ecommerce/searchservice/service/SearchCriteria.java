package com.serhat.ecommerce.searchservice.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the shopper asked for. Kept as a record so the controller does the parsing and the
 * search service takes a single well-formed argument.
 *
 * @param sort one of {@code relevance}, {@code price_asc}, {@code price_desc},
 *             {@code rating}, {@code newest}
 */
public record SearchCriteria(
        String query,
        Long categoryId,
        List<String> brands,
        List<String> attributes,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        String sort,
        int page,
        int size
) {}
