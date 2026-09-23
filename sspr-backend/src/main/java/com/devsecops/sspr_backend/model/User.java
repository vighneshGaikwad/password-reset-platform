package com.devsecops.ssprbackend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    public enum UserStatus {
        ACTIVE, LOCKED, RESET_PENDING
    }
}