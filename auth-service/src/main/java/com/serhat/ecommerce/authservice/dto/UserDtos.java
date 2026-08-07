package com.serhat.ecommerce.authservice.dto;

import com.serhat.ecommerce.authservice.model.UserEntity;

import java.util.List;

public class UserDtos {

    public record UserResponse(
            Long id,
            String username,
            String firstName,
            String lastName,
            String email,
            List<String> roles
    ) {
        public static UserResponse from(UserEntity u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail(),
                    u.getRoles().stream().map(Enum::name).toList());
        }
    }
}
