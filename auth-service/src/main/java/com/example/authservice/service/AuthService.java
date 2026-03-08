package com.example.authservice.service;

import com.example.authservice.dto.AuthDtos.*;
import com.example.authservice.model.UserEntity;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

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

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        UserEntity u = UserEntity.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .build();
        userRepo.save(u);
        String access = jwtUtil.generateAccessToken(u.getUsername());
        String refresh = jwtUtil.generateRefreshToken(u.getUsername());
        return new AuthResponse(access, refresh, "Bearer", u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        UserEntity u = userRepo.findByUsername(req.getUsername())
                .orElseThrow(() -> new NoSuchElementException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), u.getPassword())) {
            throw new NoSuchElementException("Invalid credentials");
        }
        String access = jwtUtil.generateAccessToken(u.getUsername());
        String refresh = jwtUtil.generateRefreshToken(u.getUsername());
        return new AuthResponse(access, refresh, "Bearer", u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail());
    }

    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String access = jwtUtil.generateAccessToken(username);
        String refresh = jwtUtil.generateRefreshToken(username);
        // load user profile to include in response
        UserEntity u = userRepo.findByUsername(username).orElse(null);
        if (u != null) {
            return new AuthResponse(access, refresh, "Bearer", u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail());
        }
        return new AuthResponse(access, refresh, "Bearer", username, null, null, null);
    }
}
