package com.serhat.ecommerce.authservice.service;

import com.serhat.ecommerce.authservice.dto.AuthDtos.*;
import com.serhat.ecommerce.authservice.model.Role;
import com.serhat.ecommerce.authservice.model.UserEntity;
import com.serhat.ecommerce.authservice.repository.UserRepository;
import com.serhat.ecommerce.authservice.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepo, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        // Self-registration always yields a plain shopper; SELLER/ADMIN are granted
        // out-of-band so that signing up cannot escalate into catalog management.
        Set<Role> roles = new LinkedHashSet<>();
        roles.add(Role.CUSTOMER);

        UserEntity user = UserEntity.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .roles(roles)
                .build();
        userRepo.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        UserEntity user = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new NoSuchElementException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new NoSuchElementException("Invalid credentials");
        }
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        // Rejects an access token presented here, so a leaked short-lived access token
        // cannot be exchanged for a fresh long-lived credential pair.
        if (!jwtUtil.isValidRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        UserEntity user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        // Roles are re-read from the database rather than copied from the old token, so a
        // revoked role stops applying at the next refresh instead of living out the token's week.
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(UserEntity user) {
        String access = jwtUtil.generateAccessToken(user);
        String refresh = jwtUtil.generateRefreshToken(user);
        return new AuthResponse(access, refresh, "Bearer", user.getUsername(),
                user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getRoles().stream().map(Enum::name).toList());
    }
}
