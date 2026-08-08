package com.serhat.ecommerce.searchservice.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * The denormalised search view of a product - a CQRS read model fed from catalog events,
 * never written to directly.
 *
 * <p>Everything a results page renders is copied in (brand and category names, the primary
 * image, rating) so a search returns fully-formed cards without fanning out to
 * product-service per hit.
 *
 * <p>Field types are chosen deliberately: {@code text} fields are analysed for matching,
 * while the {@code .keyword} sub-fields and the standalone keyword fields are what facet
 * aggregations and exact filters run on. Aggregating over an analysed field would bucket
 * "Apple Watch" as two separate terms.
 */
@Document(indexName = "products", versionType = Document.VersionType.EXTERNAL)
@Setting(settingPath = "elasticsearch/product-index-settings.json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String id;

    /**
     * Set to the source event's timestamp and enforced by Elasticsearch as an external
     * version. Kafka only orders within a partition and a retry can replay an older
     * message, so without this a stale update could overwrite a newer one. Elasticsearch
     * rejects a write whose version is not greater than the stored one, which turns
     * out-of-order delivery into a no-op instead of data loss.
     */
    @Version
    private Long version;

    /**
     * Indexed with a custom analyser that folds Turkish diacritics, so "kulaklik" matches
     * "kulaklık". The {@code keyword} sub-field backs exact-match sorting by name.
     */
    @Field(type = FieldType.Text, analyzer = "catalog_analyzer", searchAnalyzer = "catalog_analyzer")
    private String name;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Text, analyzer = "catalog_analyzer", searchAnalyzer = "catalog_analyzer")
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Long)
    private Long brandId;

    @Field(type = FieldType.Keyword)
    private String brandName;

    @Field(type = FieldType.Long)
    private Long sellerId;

    @Field(type = FieldType.Keyword)
    private String sellerName;

    @Field(type = FieldType.Double)
    private BigDecimal averageRating;

    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    @Field(type = FieldType.Keyword, index = false)
    private String primaryImageUrl;

    /** Variant attribute values ("Blue", "XL") flattened so they are searchable and facetable. */
    @Field(type = FieldType.Keyword)
    private List<String> attributeValues;

    @Field(type = FieldType.Keyword)
    private List<String> skus;

    /** Used to break relevance ties towards newer listings and to sort by recency. */
    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX")
    private Instant indexedAt;
}
