package com.example.authservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    // roles veya ek alanlar eklenecekse buraya
}

