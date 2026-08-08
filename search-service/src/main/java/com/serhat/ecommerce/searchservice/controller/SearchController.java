package com.serhat.ecommerce.searchservice.controller;

import com.serhat.ecommerce.searchservice.dto.SearchDtos.SearchResponse;
import com.serhat.ecommerce.searchservice.service.ProductSearchService;
import com.serhat.ecommerce.searchservice.service.SearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    /**
     * Elasticsearch refuses deep pagination past {@code max_result_window} (10 000), so the
     * page/size combination is clamped rather than letting the request fail at the engine.
     */
    private static final int MAX_PAGE_SIZE = 60;
    private static final int MAX_RESULT_WINDOW = 10_000;

    private final ProductSearchService searchService;

    @GetMapping("/products")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<String> brand,
            @RequestParam(required = false) List<String> attribute,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int maxPage = (MAX_RESULT_WINDOW / safeSize) - 1;
        int safePage = Math.min(Math.max(page, 0), Math.max(maxPage, 0));

        SearchCriteria criteria = new SearchCriteria(q, categoryId, brand, attribute,
                minPrice, maxPrice, minRating, sort, safePage, safeSize);

        return ResponseEntity.ok(searchService.search(criteria));
    }
}
