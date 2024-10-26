package com.ecommerce.vic.repository;

import com.ecommerce.vic.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    
    Optional<VerificationToken> findByTokenAndTokenType(String token, VerificationToken.TokenType tokenType);
    
    Optional<VerificationToken> findByUserIdAndTokenType(Long userId, VerificationToken.TokenType tokenType);
    
    List<VerificationToken> findByExpiryDateBeforeAndUsed(LocalDateTime now, boolean used);
    
    boolean existsByUserIdAndTokenTypeAndUsedFalseAndExpiryDateAfter(
        Long userId, 
        VerificationToken.TokenType tokenType, 
        LocalDateTime now
    );
}
