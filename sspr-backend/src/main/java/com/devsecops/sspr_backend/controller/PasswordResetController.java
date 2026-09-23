package com.devsecops.ssprbackend.controller;

import com.devsecops.ssprbackend.model.ResetToken;
import com.devsecops.ssprbackend.repository.ResetTokenRepository;
import com.devsecops.ssprbackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*") // Allows local React connection
public class PasswordResetController {

    private final UserRepository userRepository;
    private final ResetTokenRepository tokenRepository;

    public PasswordResetController(UserRepository userRepository, ResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @PostMapping("/request-reset")
    public ResponseEntity<String> requestReset(@RequestParam String email) {
        // Anti-Enumeration: Return generic success message regardless of user presence
        userRepository.findByEmail(email).ifPresent(user -> {
            String tokenValue = UUID.randomUUID().toString();
            ResetToken token = new ResetToken(
                null, tokenValue, email, LocalDateTime.now().plusMinutes(15), false
            );
            tokenRepository.save(token);
            // System print simulates sending an email notification locally
            System.out.println(">>> RESET LINK SENT TO " + email + ": Token=" + tokenValue);
        });

        return ResponseEntity.ok("If the email exists in our system, a password reset link has been sent.");
    }
}