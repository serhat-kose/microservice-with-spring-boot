package com.serhat.ecommerce.commons.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns the identity headers injected by the gateway into a Spring Security
 * {@link org.springframework.security.core.Authentication}, so downstream services can use
 * {@code @PreAuthorize} and {@link CurrentUser} instead of trusting request parameters.
 *
 * <p>Roles arrive as a comma-separated list (e.g. {@code CUSTOMER,SELLER}) and are exposed
 * as {@code ROLE_}-prefixed authorities so {@code hasRole('SELLER')} works as expected.
 *
 * <p>If the headers are absent the request is left unauthenticated and the service's own
 * authorization rules decide whether to reject it - this filter never rejects on its own.
 */
public class IdentityHeaderFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader(IdentityHeaders.USER_ID);

        if (StringUtils.hasText(userId) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = request.getHeader(IdentityHeaders.USERNAME);
            Set<String> roles = parseRoles(request.getHeader(IdentityHeaders.ROLES));

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                    .collect(Collectors.toList());

            AuthenticatedUser principal = new AuthenticatedUser(userId, username, roles);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private Set<String> parseRoles(String header) {
        if (!StringUtils.hasText(header)) {
            return Set.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(role -> role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
