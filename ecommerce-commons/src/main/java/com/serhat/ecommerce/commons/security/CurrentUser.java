package com.serhat.ecommerce.commons.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Convenience accessor for the caller identity placed in the security context by
 * {@link IdentityHeaderFilter}.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthenticatedUser> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    /**
     * @return the authenticated caller's user id
     * @throws IllegalStateException if there is no authenticated caller - this indicates a
     *         request reached a protected handler without passing through the gateway,
     *         which is a deployment/configuration fault rather than a client error.
     */
    public static String requireUserId() {
        return get().map(AuthenticatedUser::userId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user in the security context"));
    }
}
