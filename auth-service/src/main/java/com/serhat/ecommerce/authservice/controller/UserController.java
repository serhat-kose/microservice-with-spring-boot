package com.serhat.ecommerce.authservice.controller;

import com.serhat.ecommerce.authservice.dto.UserDtos.UserResponse;
import com.serhat.ecommerce.authservice.repository.UserRepository;
import com.serhat.ecommerce.commons.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** The caller's own profile - the only user-lookup an ordinary customer needs. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return userRepository.findById(Long.valueOf(CurrentUser.requireUserId()))
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Restricted to operators: previously any authenticated account could walk these two
     * endpoints and harvest every user's id, name and email.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getByUsername(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
