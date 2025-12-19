package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class AuthDtos {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshRequest {
        @NotBlank
        private String refreshToken;
    }
}

