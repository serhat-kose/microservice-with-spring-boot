package com.serhat.ecommerce.apigateway.filter;

import com.serhat.ecommerce.commons.security.IdentityHeaders;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The single place where a JWT is validated. On success the caller's identity is forwarded
 * downstream as headers so services can authorize without re-parsing the token.
 *
 * <p>Two things matter for correctness here:
 * <ul>
 *   <li>Identity headers supplied by the <em>client</em> are stripped unconditionally,
 *       before anything else. Otherwise a caller could simply send
 *       {@code X-User-Id: 1, X-User-Roles: ADMIN} and impersonate anyone, since downstream
 *       services trust those headers.</li>
 *   <li>Public paths are matched exactly rather than by prefix, so a future
 *       {@code /api/auth/...} sub-path cannot accidentally become world-readable.</li>
 * </ul>
 */
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";

    /** Endpoints a caller must be able to reach without a token in order to obtain one. */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh"
    );

    /** Catalog browsing is open to anonymous shoppers, as on any consumer storefront. */
    private static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/products",
            "/api/categories",
            "/api/search"
    );

    private final Key signingKey;

    public JwtAuthenticationGatewayFilter(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Always drop client-supplied identity headers first - they are only ever
        // legitimate when this filter sets them.
        ServerHttpRequest.Builder sanitized = request.mutate()
                .headers(headers -> {
                    headers.remove(IdentityHeaders.USER_ID);
                    headers.remove(IdentityHeaders.USERNAME);
                    headers.remove(IdentityHeaders.ROLES);
                });

        String correlationId = request.getHeaders().getFirst(IdentityHeaders.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        sanitized.header(IdentityHeaders.CORRELATION_ID, correlationId);

        String authHeader = request.getHeaders().getFirst("Authorization");
        boolean anonymousAllowed = isPublic(path, request.getMethod());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (anonymousAllowed) {
                return chain.filter(exchange.mutate().request(sanitized.build()).build());
            }
            return reject(exchange);
        }

        Claims claims;
        try {
            claims = Jwts.parserBuilder().setSigningKey(signingKey).build()
                    .parseClaimsJws(authHeader.substring(7)).getBody();
        } catch (Exception e) {
            // An invalid token is rejected even on public paths: sending a bad credential
            // is a client error worth surfacing rather than silently downgrading to anonymous.
            return reject(exchange);
        }

        // A refresh token must not be usable as an access credential.
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            return reject(exchange);
        }

        String userId = claims.get(CLAIM_USER_ID, String.class);
        Object roles = claims.get(CLAIM_ROLES);

        sanitized.header(IdentityHeaders.USER_ID, userId == null ? "" : userId);
        sanitized.header(IdentityHeaders.USERNAME, claims.getSubject() == null ? "" : claims.getSubject());
        sanitized.header(IdentityHeaders.ROLES, formatRoles(roles));

        return chain.filter(exchange.mutate().request(sanitized.build()).build());
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        if (HttpMethod.GET.equals(method)) {
            return PUBLIC_GET_PREFIXES.stream()
                    .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
        }
        return false;
    }

    private String formatRoles(Object roles) {
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        }
        return roles == null ? "" : String.valueOf(roles);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
