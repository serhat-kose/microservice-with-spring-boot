package com.serhat.ecommerce.searchservice.repository;

import com.serhat.ecommerce.searchservice.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Simple by-id access. Query-time search with filters and facets is built explicitly in
 * {@code ProductSearchService} instead, because derived query methods cannot express
 * aggregations or relevance boosting.
 */
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
}
