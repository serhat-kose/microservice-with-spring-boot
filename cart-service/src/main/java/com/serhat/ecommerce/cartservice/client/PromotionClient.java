package com.serhat.ecommerce.cartservice.client;

import com.serhat.ecommerce.commons.security.AuthenticatedUser;
import com.serhat.ecommerce.commons.security.CurrentUser;
import com.serhat.ecommerce.commons.security.IdentityHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls promotion-service to price a coupon.
 *
 * <p>Resolved through Eureka (`lb://`) rather than a fixed host, so it load-balances across
 * promotion-service replicas instead of pinning to one.
 *
 * <p>The caller's identity is forwarded explicitly because promotion-service enforces
 * per-user coupon limits and would otherwise see an anonymous request. Service-to-service
 * calls do not carry the gateway's headers on their own.
 */
@Slf4j
@Component
public class PromotionClient {

    private static final String BASE_URL = "http://promotion-service/api/promotions";

    private final RestTemplate restTemplate;

    public PromotionClient(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CouponQuote validate(String code, BigDecimal orderAmount) {
        return call("/validate", Map.of("code", code, "orderAmount", orderAmount));
    }

    public CouponQuote redeem(String code, BigDecimal orderAmount, String orderId) {
        return call("/redeem", Map.of("code", code, "orderAmount", orderAmount, "orderId", orderId));
    }

    @SuppressWarnings("unchecked")
    private CouponQuote call(String path, Map<String, Object> body) {
        try {
            Map<String, Object> response = restTemplate.exchange(
                    BASE_URL + path, HttpMethod.POST,
                    new HttpEntity<>(body, identityHeaders()), Map.class).getBody();

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty response from promotion-service");
            }

            boolean valid = Boolean.TRUE.equals(response.get("valid"));
            String reason = (String) response.get("reason");
            BigDecimal discount = response.get("discountAmount") == null
                    ? BigDecimal.ZERO
                    : new BigDecimal(String.valueOf(response.get("discountAmount")));

            return new CouponQuote(valid, reason, discount);
        } catch (RestClientException e) {
            // Deliberately fails the request rather than quietly continuing without the
            // discount: charging a shopper full price for a coupon they were shown as valid
            // is worse than asking them to retry.
            log.warn("promotion-service call to {} failed: {}", path, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Coupons are temporarily unavailable, please try again");
        }
    }

    private HttpHeaders identityHeaders() {
        HttpHeaders headers = new HttpHeaders();
        CurrentUser.get().ifPresent(user -> {
            headers.set(IdentityHeaders.USER_ID, user.userId());
            if (user.username() != null) {
                headers.set(IdentityHeaders.USERNAME, user.username());
            }
            headers.set(IdentityHeaders.ROLES, String.join(",", user.roles()));
        });
        return headers;
    }

    public record CouponQuote(boolean valid, String reason, BigDecimal discountAmount) {}
}
