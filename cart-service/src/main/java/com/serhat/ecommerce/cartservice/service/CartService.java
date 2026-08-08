package com.serhat.ecommerce.cartservice.service;

import com.serhat.ecommerce.cartservice.client.PromotionClient;
import com.serhat.ecommerce.cartservice.dto.CartDtos;
import com.serhat.ecommerce.cartservice.exception.CartNotFoundException;
import com.serhat.ecommerce.cartservice.model.Cart;
import com.serhat.ecommerce.cartservice.model.CartItem;
import com.serhat.ecommerce.cartservice.model.Product;
import com.serhat.ecommerce.cartservice.repository.CartRepository;
import com.serhat.ecommerce.cartservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhat.ecommerce.commons.event.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;
    private final PromotionClient promotionClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** A cart plus its current price breakdown, including any applied coupon. */
    public record PricedCart(Cart cart, PricingService.Quote quote, String couponMessage) {}

    @Transactional(readOnly = true)
    public PricedCart getPricedCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> newCart(userId));
        return price(cart);
    }

    /**
     * Resolves the product and its price from the local catalog read-model - fed by
     * {@code product-created}/{@code product-updated} events - rather than trusting the
     * request. The read-model was previously written by the event listener but never read,
     * so the client's own price was what got stored and charged.
     */
    @Transactional
    public PricedCart addItem(String userId, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown product: " + productId));

        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> newCart(userId));

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            item.setPrice(product.getPrice());
            item.setProductName(product.getName());
        } else {
            cart.getItems().add(new CartItem(productId, product.getName(), quantity, product.getPrice()));
        }
        return price(cartRepository.save(cart));
    }

    @Transactional
    public PricedCart removeItem(String userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new CartNotFoundException(userId));
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        return price(cartRepository.save(cart));
    }

    /**
     * Applies a coupon after checking it against the current basket, so an invalid code is
     * rejected immediately rather than at checkout.
     */
    @Transactional
    public PricedCart applyCoupon(String userId, String code) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new CartNotFoundException(userId));
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        PromotionClient.CouponQuote quote = promotionClient.validate(code, pricingService.subtotal(cart));
        if (!quote.valid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    quote.reason() == null ? "Coupon cannot be applied" : quote.reason());
        }

        cart.setCouponCode(code.toUpperCase());
        return price(cartRepository.save(cart));
    }

    @Transactional
    public PricedCart removeCoupon(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new CartNotFoundException(userId));
        cart.setCouponCode(null);
        return price(cartRepository.save(cart));
    }

    /**
     * Re-prices every line against the catalog immediately before publishing, so a price
     * change while the cart sat idle cannot be checked out at the stale price.
     *
     * <p>Waits for the Kafka send to be acknowledged before clearing the cart, so a broker
     * failure leaves the cart intact instead of silently dropping the order.
     */
    @Transactional
    public void checkout(String userId, CartDtos.AddressDto shippingAddress) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new CartNotFoundException(userId));
        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        for (CartItem item : cart.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Product is no longer available: " + item.getProductId()));
            item.setPrice(product.getPrice());
            item.setProductName(product.getName());
        }

        PricedCart priced = price(cart);
        PricingService.Quote quote = priced.quote();

        CartDtos.CheckoutEvent event = new CartDtos.CheckoutEvent(
                userId, cart.getItems(), shippingAddress, quote.getCouponCode(),
                quote.getDiscountAmount(), quote.getShippingCost(), quote.getTaxAmount(),
                quote.getTotalAmount());

        try {
            EventEnvelope<Object> envelope = EventEnvelope.of("cart.checkout", null, event);
            kafkaTemplate.send("cart-checkout", userId, objectMapper.writeValueAsString(envelope))
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to publish cart-checkout event for user {}", userId, e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Checkout is temporarily unavailable, please retry");
        }

        cart.getItems().clear();
        cart.setCouponCode(null);
        cartRepository.save(cart);
    }

    /**
     * Prices the cart, re-quoting any applied coupon against the current subtotal.
     *
     * <p>A coupon that has since expired or no longer qualifies is dropped with a message
     * rather than failing the page, so a shopper is never shown a discount they will not get.
     */
    private PricedCart price(Cart cart) {
        BigDecimal subtotal = pricingService.subtotal(cart);

        if (!StringUtils.hasText(cart.getCouponCode()) || cart.getItems().isEmpty()) {
            return new PricedCart(cart, pricingService.quote(cart, BigDecimal.ZERO, null), null);
        }

        try {
            PromotionClient.CouponQuote quote = promotionClient.validate(cart.getCouponCode(), subtotal);
            if (quote.valid()) {
                return new PricedCart(cart,
                        pricingService.quote(cart, quote.discountAmount(), cart.getCouponCode()), null);
            }
            return new PricedCart(cart, pricingService.quote(cart, BigDecimal.ZERO, null),
                    quote.reason() == null ? "Coupon is no longer valid" : quote.reason());
        } catch (ResponseStatusException e) {
            // promotion-service unreachable: show the basket without a discount and say so,
            // rather than blocking the shopper from seeing their cart at all.
            return new PricedCart(cart, pricingService.quote(cart, BigDecimal.ZERO, null),
                    "Coupon could not be checked right now");
        }
    }

    private Cart newCart(String userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        return cart;
    }
}
