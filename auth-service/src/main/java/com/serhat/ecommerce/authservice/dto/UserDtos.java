package com.serhat.ecommerce.authservice.dto;

import com.serhat.ecommerce.authservice.model.UserEntity;

public class UserDtos {

    public record UserResponse(
            Long id,
            String username,
            String firstName,
            String lastName,
            String email
    ) {
        public static UserResponse from(UserEntity u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(), u.getEmail());
        }
    }
}
