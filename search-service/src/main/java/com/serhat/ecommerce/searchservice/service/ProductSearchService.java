package com.serhat.ecommerce.searchservice.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.serhat.ecommerce.searchservice.document.ProductDocument;
import com.serhat.ecommerce.searchservice.dto.SearchDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final String AGG_BRANDS = "brands";
    private static final String AGG_CATEGORIES = "categories";
    private static final String AGG_ATTRIBUTES = "attributes";
    private static final String AGG_PRICE_STATS = "price_stats";

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchResponse search(SearchCriteria criteria) {
        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(buildQuery(criteria))
                .withAggregation(AGG_BRANDS, termsAggregation("brandName", 30))
                .withAggregation(AGG_CATEGORIES, termsAggregation("categoryName", 30))
                .withAggregation(AGG_ATTRIBUTES, termsAggregation("attributeValues", 50))
                .withAggregation(AGG_PRICE_STATS, Aggregation.of(a -> a.stats(s -> s.field("price"))))
                .withPageable(PageRequest.of(criteria.page(), criteria.size()));

        applySort(builder, criteria.sort());

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(builder.build(), ProductDocument.class);

        List<SearchHitDto> results = hits.getSearchHits().stream()
                .map(hit -> SearchHitDto.from(hit.getContent(), hit.getScore() == 0f ? null : (double) hit.getScore()))
                .toList();

        int totalPages = criteria.size() == 0 ? 0
                : (int) Math.ceil((double) hits.getTotalHits() / criteria.size());

        return new SearchResponse(results, hits.getTotalHits(), criteria.page(), criteria.size(),
                totalPages, extractFacets(hits), extractPriceRange(hits));
    }

    /**
     * Free text goes into {@code must} so it contributes to the relevance score, while every
     * refinement goes into {@code filter}. Filters are not scored and are cacheable by
     * Elasticsearch, which is what keeps a heavily-faceted query fast.
     */
    private Query buildQuery(SearchCriteria criteria) {
        List<Query> must = new ArrayList<>();
        List<Query> filters = new ArrayList<>();

        if (StringUtils.hasText(criteria.query())) {
            String text = criteria.query();
            must.add(Query.of(q -> q.multiMatch(m -> m
                    // Name is boosted over description so a keyword in the title outranks a
                    // passing mention in the body copy.
                    .fields("name^3", "description")
                    .query(text)
                    .type(TextQueryType.BestFields)
                    // Tolerates a single typo on short words and two on longer ones, which is
                    // what "kulaklik" vs "kulaklık" style near-misses need.
                    .fuzziness("AUTO")
                    .prefixLength(1))));
        } else {
            must.add(Query.of(q -> q.matchAll(m -> m)));
        }

        if (criteria.categoryId() != null) {
            filters.add(termFilter("categoryId", criteria.categoryId()));
        }
        if (!CollectionUtils.isEmpty(criteria.brands())) {
            filters.add(termsFilter("brandName", criteria.brands()));
        }
        if (!CollectionUtils.isEmpty(criteria.attributes())) {
            // Multiple attribute values are OR-ed within the facet, matching how a shopper
            // reads "Blue or Black", not "Blue and Black".
            filters.add(termsFilter("attributeValues", criteria.attributes()));
        }
        if (criteria.minPrice() != null || criteria.maxPrice() != null) {
            filters.add(Query.of(q -> q.range(r -> {
                r.field("price");
                if (criteria.minPrice() != null) {
                    r.gte(co.elastic.clients.json.JsonData.of(criteria.minPrice().doubleValue()));
                }
                if (criteria.maxPrice() != null) {
                    r.lte(co.elastic.clients.json.JsonData.of(criteria.maxPrice().doubleValue()));
                }
                return r;
            })));
        }
        if (criteria.minRating() != null) {
            filters.add(Query.of(q -> q.range(r -> r.field("averageRating")
                    .gte(co.elastic.clients.json.JsonData.of(criteria.minRating().doubleValue())))));
        }

        return Query.of(q -> q.bool(b -> b.must(must).filter(filters)));
    }

    private void applySort(NativeQueryBuilder builder, String sort) {
        if (sort == null) {
            return;
        }
        switch (sort) {
            case "price_asc" -> builder.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Asc)));
            case "price_desc" -> builder.withSort(s -> s.field(f -> f.field("price").order(SortOrder.Desc)));
            case "rating" -> builder.withSort(s -> s.field(f -> f.field("averageRating").order(SortOrder.Desc)));
            case "newest" -> builder.withSort(s -> s.field(f -> f.field("indexedAt").order(SortOrder.Desc)));
            // "relevance" (and anything unrecognised) leaves Elasticsearch's default
            // _score ordering in place.
            default -> { }
        }
    }

    private Aggregation termsAggregation(String field, int size) {
        return Aggregation.of(a -> a.terms(t -> t.field(field).size(size)));
    }

    private Query termFilter(String field, Object value) {
        return Query.of(q -> q.term(t -> t.field(field).value(String.valueOf(value))));
    }

    private Query termsFilter(String field, List<String> values) {
        return Query.of(q -> q.terms(t -> t.field(field)
                .terms(v -> v.value(values.stream()
                        .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                        .toList()))));
    }

    private List<Facet> extractFacets(SearchHits<ProductDocument> hits) {
        Map<String, Aggregate> aggregations = rawAggregations(hits);
        if (aggregations.isEmpty()) {
            return List.of();
        }
        List<Facet> facets = new ArrayList<>();
        addTermsFacet(facets, aggregations, AGG_BRANDS);
        addTermsFacet(facets, aggregations, AGG_CATEGORIES);
        addTermsFacet(facets, aggregations, AGG_ATTRIBUTES);
        return facets;
    }

    private void addTermsFacet(List<Facet> facets, Map<String, Aggregate> aggregations, String name) {
        Aggregate aggregate = aggregations.get(name);
        if (aggregate == null || !aggregate.isSterms()) {
            return;
        }
        List<FacetValue> values = aggregate.sterms().buckets().array().stream()
                .map(this::toFacetValue)
                .toList();
        if (!values.isEmpty()) {
            facets.add(new Facet(name, values));
        }
    }

    private FacetValue toFacetValue(StringTermsBucket bucket) {
        return new FacetValue(bucket.key().stringValue(), bucket.docCount());
    }

    /** Powers the price slider's bounds, so the UI does not have to guess a range. */
    private PriceRange extractPriceRange(SearchHits<ProductDocument> hits) {
        Aggregate aggregate = rawAggregations(hits).get(AGG_PRICE_STATS);
        if (aggregate == null || !aggregate.isStats()) {
            return null;
        }
        var stats = aggregate.stats();
        if (stats.count() == 0) {
            return null;
        }
        return new PriceRange(BigDecimal.valueOf(stats.min()), BigDecimal.valueOf(stats.max()));
    }

    private Map<String, Aggregate> rawAggregations(SearchHits<ProductDocument> hits) {
        if (!(hits.getAggregations() instanceof ElasticsearchAggregations elasticsearchAggregations)) {
            return Map.of();
        }
        Map<String, Aggregate> result = new java.util.LinkedHashMap<>();
        elasticsearchAggregations.aggregations()
                .forEach(container -> result.put(container.aggregation().getName(),
                        container.aggregation().getAggregate()));
        return result;
    }
}
