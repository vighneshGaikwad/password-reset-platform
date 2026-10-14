package com.devsecops.ssprbackend.controller;

import com.devsecops.ssprbackend.model.ResetToken;
import com.devsecops.ssprbackend.model.User;
import com.devsecops.ssprbackend.repository.ResetTokenRepository;
import com.devsecops.ssprbackend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final UserRepository userRepository;
    private final ResetTokenRepository tokenRepository;

    public AdminController(UserRepository userRepository, ResetTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    // 1. CREATE Record (New User)
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userRepository.save(user);
        return ResponseEntity.ok(savedUser);
    }

    // 2. VIEW & SEARCH Records
    @GetMapping("/requests")
    public ResponseEntity<List<ResetToken>> getAllOrSearchRequests(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(tokenRepository.findByUserEmailContainingIgnoreCase(search));
        }
        return ResponseEntity.ok(tokenRepository.findAll());
    }

    // 3. UPDATE Workflow Status (Role-based status transitions)
    @PutMapping("/requests/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam ResetToken.RequestStatus status) {
        Optional<ResetToken> tokenOpt = tokenRepository.findById(id);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ResetToken token = tokenOpt.get();
        token.setStatus(status);
        if (status == ResetToken.RequestStatus.COMPLETED) {
            token.setUsed(true);
        }
        tokenRepository.save(token);

        return ResponseEntity.ok(Map.of("message", "Status updated successfully", "status", status));
    }

    // 4. SUMMARY DASHBOARD
    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalUsers", userRepository.count());
        metrics.put("totalRequests", tokenRepository.count());
        metrics.put("pendingRequests", tokenRepository.countByStatus(ResetToken.RequestStatus.PENDING));
        metrics.put("completedRequests", tokenRepository.countByStatus(ResetToken.RequestStatus.COMPLETED));
        metrics.put("rejectedRequests", tokenRepository.countByStatus(ResetToken.RequestStatus.REJECTED));
        return ResponseEntity.ok(metrics);
    }
}