package com.serhat.ecommerce.cartservice.model;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    private Long productId;

    /**
     * Snapshot of the product name at the time it was added, so the cart can be rendered
     * without a lookup per line and still reads correctly if the product is later renamed.
     */
    private String productName;

    private Integer quantity;

    /** Unit price resolved server-side from the catalog when the item was added. */
    private BigDecimal price;
}
