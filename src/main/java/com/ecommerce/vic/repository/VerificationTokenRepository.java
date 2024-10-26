package com.ecommerce.vic.repository;

import com.ecommerce.vic.model.User;
import com.ecommerce.vic.model.VerificationToken;
import io.micrometer.observation.ObservationFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByTokenAndTokenType(String token, VerificationToken.TokenType tokenType);

    boolean existsByUserAndTokenTypeAndUsedFalseAndExpiryDateAfter(
            User user,
            VerificationToken.TokenType tokenType,
            LocalDateTime now
    );

    List<VerificationToken> findByExpiryDateBeforeAndUsed(LocalDateTime now, boolean used);

    Optional<VerificationToken> findByUserAndTokenType(User user, VerificationToken.TokenType tokenType);
}
