package com.devsecops.ssprbackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private boolean used = false;
}