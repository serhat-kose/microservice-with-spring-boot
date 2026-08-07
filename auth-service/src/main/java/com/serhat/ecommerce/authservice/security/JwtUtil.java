package com.serhat.ecommerce.authservice.security;

import com.serhat.ecommerce.authservice.model.Role;
import com.serhat.ecommerce.authservice.model.UserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class JwtUtil {

    /** Claim carrying the stable user id, so downstream services key on it rather than a mutable username. */
    public static final String CLAIM_USER_ID = "uid";
    /** Claim carrying the user's roles, forwarded downstream by the gateway for authorization. */
    public static final String CLAIM_ROLES = "roles";
    /** Distinguishes an access token from a refresh token so the two cannot be used interchangeably. */
    public static final String CLAIM_TOKEN_TYPE = "typ";

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final Key key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = 15 * 60 * 1000L; // 15 minutes
        this.refreshTokenValidityMs = 7 * 24 * 60 * 60 * 1000L; // 7 days
    }

    public String generateAccessToken(UserEntity user) {
        return buildToken(user, TYPE_ACCESS, accessTokenValidityMs);
    }

    public String generateRefreshToken(UserEntity user) {
        return buildToken(user, TYPE_REFRESH, refreshTokenValidityMs);
    }

    private String buildToken(UserEntity user, String tokenType, long validityMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim(CLAIM_USER_ID, String.valueOf(user.getId()))
                .claim(CLAIM_ROLES, user.getRoles().stream().map(Enum::name).toList())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        return parse(token) != null;
    }

    /**
     * Validates that the token is both well-formed and actually a refresh token. Without the
     * type check a long-lived refresh token and a short-lived access token are interchangeable,
     * so a leaked access token could be traded for fresh credentials indefinitely.
     */
    public boolean isValidRefreshToken(String token) {
        Claims claims = parse(token);
        return claims != null && TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parse(token);
        return claims == null ? null : claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public Set<Role> getRolesFromToken(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return Set.of();
        }
        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        if (roles == null) {
            return Set.of();
        }
        Set<Role> result = new LinkedHashSet<>();
        for (String role : roles) {
            try {
                result.add(Role.valueOf(role));
            } catch (IllegalArgumentException ignored) {
                // A role that no longer exists in this deployment is simply not granted.
            }
        }
        return result;
    }

    private Claims parse(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
