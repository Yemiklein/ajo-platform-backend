package com.ajo.platform.modules.auth.repository;

import com.ajo.platform.modules.auth.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByOtpAndIdentifierAndUsedFalse(String otp, String identifier);
    void deleteByIdentifier(String identifier);
}