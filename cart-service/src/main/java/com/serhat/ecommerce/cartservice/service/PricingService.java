package com.serhat.ecommerce.cartservice.service;

import com.serhat.ecommerce.cartservice.model.Cart;
import com.serhat.ecommerce.cartservice.model.CartItem;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Works out what a basket costs: subtotal, discount, shipping, tax and the payable total.
 *
 * <p>Kept in one place so the quote a shopper sees on the cart page and the figures written
 * onto the order at checkout are produced by the same code - computing them separately is
 * how a checkout ends up charging something different from what was displayed.
 */
@Service
public class PricingService {

    /** VAT rate as a percentage, e.g. 20 for 20%. */
    @Value("${ecommerce.pricing.tax-rate-percent:20}")
    private BigDecimal taxRatePercent;

    @Value("${ecommerce.pricing.shipping-cost:29.99}")
    private BigDecimal shippingCost;

    /** Baskets at or above this subtotal ship free. */
    @Value("${ecommerce.pricing.free-shipping-threshold:500.00}")
    private BigDecimal freeShippingThreshold;

    public BigDecimal subtotal(Cart cart) {
        return cart.getItems().stream()
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal lineTotal(CartItem item) {
        return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    /**
     * Shipping is charged on the discounted subtotal, so a coupon that drops the basket
     * below the free-shipping threshold also removes the free shipping - otherwise a
     * discount silently buys free delivery too.
     */
    public BigDecimal shippingFor(BigDecimal discountedSubtotal) {
        if (discountedSubtotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return discountedSubtotal.compareTo(freeShippingThreshold) >= 0
                ? BigDecimal.ZERO
                : shippingCost.setScale(2, RoundingMode.HALF_UP);
    }

    /** Tax applies to goods after discount plus shipping. */
    public BigDecimal taxFor(BigDecimal discountedSubtotal, BigDecimal shipping) {
        return discountedSubtotal.add(shipping)
                .multiply(taxRatePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Full quote for a basket, given a discount already agreed with promotion-service. */
    public Quote quote(Cart cart, BigDecimal discount, String couponCode) {
        BigDecimal subtotal = subtotal(cart);
        BigDecimal appliedDiscount = discount == null ? BigDecimal.ZERO : discount.min(subtotal);
        BigDecimal discounted = subtotal.subtract(appliedDiscount);
        BigDecimal shipping = shippingFor(discounted);
        BigDecimal tax = taxFor(discounted, shipping);
        BigDecimal total = discounted.add(shipping).add(tax).setScale(2, RoundingMode.HALF_UP);
        return new Quote(subtotal, appliedDiscount, couponCode, shipping, tax, total);
    }

    @Getter
    public static class Quote {
        private final BigDecimal subtotal;
        private final BigDecimal discountAmount;
        private final String couponCode;
        private final BigDecimal shippingCost;
        private final BigDecimal taxAmount;
        private final BigDecimal totalAmount;

        public Quote(BigDecimal subtotal, BigDecimal discountAmount, String couponCode,
                     BigDecimal shippingCost, BigDecimal taxAmount, BigDecimal totalAmount) {
            this.subtotal = subtotal;
            this.discountAmount = discountAmount;
            this.couponCode = couponCode;
            this.shippingCost = shippingCost;
            this.taxAmount = taxAmount;
            this.totalAmount = totalAmount;
        }
    }
}
