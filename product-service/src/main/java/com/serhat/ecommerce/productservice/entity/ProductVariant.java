package com.serhat.ecommerce.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A concrete purchasable variation of a product - "this shirt, in blue, size M".
 *
 * <p>The {@code sku} is the identifier the rest of the platform buys and reserves against:
 * a customer adds a variant to the cart, and stock is held per SKU, not per product.
 * Tracking stock on the product would make "size M sold out" unrepresentable.
 *
 * <p>Attributes are a free-form map rather than fixed columns so different categories can
 * describe themselves differently (clothing has size/colour, phones have storage/colour)
 * without schema changes per category.
 */
@Entity
@Table(name = "product_variants", indexes = {
        @Index(name = "idx_product_variants_sku", columnList = "sku", unique = true),
        @Index(name = "idx_product_variants_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku;

    /**
     * Overrides the product's base price when set; null means "same as the product".
     * Keeps the common case (all variants priced alike) free of duplication.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    /** Original price before a discount, shown struck through. Null when not discounted. */
    @Column(precision = 12, scale = 2)
    private BigDecimal listPrice;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_variant_attributes",
            joinColumns = @JoinColumn(name = "variant_id"))
    @MapKeyColumn(name = "attribute_name")
    @Column(name = "attribute_value")
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /** The price a customer actually pays for this variant. */
    public BigDecimal effectivePrice() {
        return price != null ? price : product.getBasePrice();
    }
}
