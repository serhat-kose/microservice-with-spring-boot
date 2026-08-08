package com.serhat.ecommerce.searchservice.indexing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.searchservice.document.ProductDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Projects catalog events into the search index.
 *
 * <p>Only ACTIVE products are searchable: a DRAFT has never been listed and an INACTIVE one
 * has been withdrawn, so both are removed from the index rather than filtered out at query
 * time - keeping them out means every search avoids paying for a status filter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductIndexer {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ObjectMapper objectMapper;

    public void index(JsonNode payload, Instant occurredAt) {
        long id = payload.path("id").asLong();
        if (id == 0) {
            throw new IllegalArgumentException("Product event carries no id");
        }

        String status = payload.path("status").asText(null);
        if (!"ACTIVE".equals(status)) {
            delete(id);
            return;
        }

        ProductDocument document = ProductDocument.builder()
                .id(String.valueOf(id))
                .version(occurredAt.toEpochMilli())
                .name(text(payload, "name"))
                .slug(text(payload, "slug"))
                .description(text(payload, "description"))
                .price(decimal(payload, "price"))
                .status(status)
                .categoryId(longOrNull(payload, "categoryId"))
                .categoryName(text(payload, "categoryName"))
                .brandId(longOrNull(payload, "brandId"))
                .brandName(text(payload, "brandName"))
                .sellerId(longOrNull(payload, "sellerId"))
                .sellerName(text(payload, "sellerName"))
                .averageRating(decimal(payload, "averageRating"))
                .reviewCount(payload.path("reviewCount").asInt(0))
                .primaryImageUrl(firstImageUrl(payload))
                .attributeValues(attributeValues(payload))
                .skus(skus(payload))
                .indexedAt(Instant.now())
                .build();

        try {
            elasticsearchOperations.save(document);
        } catch (OptimisticLockingFailureException e) {
            // The index already holds a newer version of this product, so this message is a
            // replay or arrived out of order. Dropping it is the correct outcome.
            log.debug("Skipped stale update for product {} (event at {})", id, occurredAt);
        }
    }

    public void delete(long productId) {
        elasticsearchOperations.delete(String.valueOf(productId), ProductDocument.class);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asLong();
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return new BigDecimal(value.asText("0"));
    }

    private String firstImageUrl(JsonNode payload) {
        JsonNode images = payload.get("imageUrls");
        if (images == null || !images.isArray() || images.isEmpty()) {
            return null;
        }
        return images.get(0).asText(null);
    }

    /** Flattens every variant's attribute values so "Blue" and "XL" become facetable terms. */
    private List<String> attributeValues(JsonNode payload) {
        Set<String> values = new LinkedHashSet<>();
        JsonNode variants = payload.get("variants");
        if (variants != null && variants.isArray()) {
            for (JsonNode variant : variants) {
                JsonNode attributes = variant.get("attributes");
                if (attributes != null && attributes.isObject()) {
                    attributes.fields().forEachRemaining(entry -> {
                        if (!entry.getValue().isNull()) {
                            values.add(entry.getValue().asText());
                        }
                    });
                }
            }
        }
        return new ArrayList<>(values);
    }

    private List<String> skus(JsonNode payload) {
        List<String> skus = new ArrayList<>();
        JsonNode variants = payload.get("variants");
        if (variants != null && variants.isArray()) {
            for (JsonNode variant : variants) {
                JsonNode sku = variant.get("sku");
                if (sku != null && !sku.isNull()) {
                    skus.add(sku.asText());
                }
            }
        }
        return skus;
    }
}
