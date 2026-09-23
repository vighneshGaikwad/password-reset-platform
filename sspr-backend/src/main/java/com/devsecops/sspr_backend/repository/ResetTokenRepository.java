package com.devsecops.ssprbackend.repository;

import com.devsecops.ssprbackend.model.ResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResetTokenRepository extends JpaRepository<ResetToken, Long> {
    Optional<ResetToken> findByToken(String token);
    List<ResetToken> findByUserEmailContainingIgnoreCase(String email);
    long countByStatus(ResetToken.RequestStatus status);
}