package com.serhat.ecommerce.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    /**
     * Buckets requests by client IP so the Redis-backed RequestRateLimiter can throttle
     * per caller instead of sharing one global bucket - important once the gateway is
     * scaled to multiple instances, since a per-instance in-memory limiter wouldn't
     * enforce a consistent limit across them.
     */
    @Bean
    public KeyResolver clientKeyResolver() {
        return exchange -> {
            String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return Mono.just(forwardedFor.split(",")[0].trim());
            }
            var remoteAddress = exchange.getRequest().getRemoteAddress();
            String ip = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
            return Mono.just(ip);
        };
    }
}
