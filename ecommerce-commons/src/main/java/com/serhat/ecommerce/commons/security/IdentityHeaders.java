package com.serhat.ecommerce.commons.security;

/**
 * Header names the API gateway uses to forward the authenticated caller's identity
 * to downstream services.
 *
 * <p><strong>Trust boundary:</strong> these headers are only trustworthy because the
 * gateway strips any client-supplied copy before injecting its own (see the gateway's
 * {@code JwtAuthenticationGatewayFilter}). Downstream services must therefore never be
 * reachable directly from outside the cluster - in Docker Compose only the gateway
 * publishes a host port, and in Kubernetes only the gateway sits behind the Ingress.
 */
public final class IdentityHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USERNAME = "X-User-Name";
    public static final String ROLES = "X-User-Roles";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private IdentityHeaders() {
    }
}
