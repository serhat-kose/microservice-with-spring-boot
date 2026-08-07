package com.serhat.ecommerce.commons.security;

import java.util.Set;

/**
 * The caller's identity as forwarded by the gateway. Held as the principal of the
 * {@link org.springframework.security.core.Authentication} so controllers can obtain it
 * via {@link CurrentUser} instead of trusting a userId path variable from the request.
 */
public record AuthenticatedUser(String userId, String username, Set<String> roles) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
