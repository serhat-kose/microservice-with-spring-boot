package com.serhat.ecommerce.apigateway.config;

import com.serhat.ecommerce.commons.security.IdentityHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimiterConfig {

    /**
     * When true, the first hop of {@code X-Forwarded-For} is used as the client address.
     * Only enable this when the gateway genuinely sits behind a trusted proxy/load balancer
     * that overwrites the header - otherwise any caller can rotate the value and evade the
     * limit entirely. Defaults to false.
     */
    @Value("${gateway.trust-forwarded-for:false}")
    private boolean trustForwardedFor;

    /**
     * Buckets requests per authenticated user when we know who is calling, falling back to
     * the network address for anonymous traffic.
     *
     * <p>Keying on the user id first matters because many customers share an address behind
     * NAT/corporate proxies - a purely IP-based limit throttles them collectively. The
     * {@code X-User-Id} header is trustworthy here because {@code JwtAuthenticationGatewayFilter}
     * strips any client-supplied copy before setting it, and runs earlier in the chain.
     */
    @Bean
    public KeyResolver clientKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst(IdentityHeaders.USER_ID);
            if (StringUtils.hasText(userId)) {
                return Mono.just("user:" + userId);
            }

            if (trustForwardedFor) {
                String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
                if (StringUtils.hasText(forwardedFor)) {
                    return Mono.just("ip:" + forwardedFor.split(",")[0].trim());
                }
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String ip = remoteAddress != null && remoteAddress.getAddress() != null
                    ? remoteAddress.getAddress().getHostAddress()
                    : "unknown";
            return Mono.just("ip:" + ip);
        };
    }
}
