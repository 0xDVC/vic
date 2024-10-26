package com.ecommerce.vic.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.vic.dto.auth.PasswordResetConfirmation;
import com.ecommerce.vic.exception.InvalidTokenException;
import com.ecommerce.vic.exception.ResourceNotFoundException;
import com.ecommerce.vic.model.User;
import com.ecommerce.vic.model.VerificationToken;
import com.ecommerce.vic.repository.UserRepository;
import com.ecommerce.vic.repository.VerificationTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;

    public void sendEmailVerification(User user) {
        log.debug("Attempting to send email verification for user: {}", user.getEmail());
        
        if (tokenRepository.existsByUserAndTokenTypeAndUsedFalseAndExpiryDateAfter(
                user, VerificationToken.TokenType.EMAIL_VERIFICATION, LocalDateTime.now())) {
            log.warn("Active verification token already exists for user: {}", user.getEmail());
            throw new IllegalStateException("Active verification token already exists");
        }

        String token = generateVerificationToken();
        saveVerificationToken(user, token, VerificationToken.TokenType.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), token);
        log.info("Email verification sent successfully to: {}", user.getEmail());
    }

    public void sendSmsVerification(User user) {
        // Fixed token type for SMS verification
        if (tokenRepository.existsByUserAndTokenTypeAndUsedFalseAndExpiryDateAfter(
                user,
                VerificationToken.TokenType.SMS_VERIFICATION,  // Changed from EMAIL to SMS
                LocalDateTime.now())) {
            throw new IllegalStateException("Active verification code already exists");
        }

        String code = generateSmsCode();
        saveVerificationToken(user, code, VerificationToken.TokenType.SMS_VERIFICATION);
        smsService.sendVerificationSms(user.getPhoneNumber(), code);
    }

    @Transactional
    public void verifyEmail(String email, String token) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken verificationToken = tokenRepository.findByTokenAndTokenType(
                        token, VerificationToken.TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        validateTokenAndUser(verificationToken, user);

        user.setEmailVerified(true);
        verificationToken.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(verificationToken);
    }

    @Transactional
    public void verifyPhone(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken verificationToken = tokenRepository.findByTokenAndTokenType(
                        code, VerificationToken.TokenType.SMS_VERIFICATION)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification code"));

        validateTokenAndUser(verificationToken, user);

        user.setPhoneVerified(true);
        verificationToken.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(verificationToken);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Fixed token type for password reset
        if (tokenRepository.existsByUserAndTokenTypeAndUsedFalseAndExpiryDateAfter(
                user,
                VerificationToken.TokenType.PASSWORD_RESET,  // Changed from EMAIL to PASSWORD_RESET
                LocalDateTime.now())) {
            throw new IllegalStateException("Active password reset token already exists");
        }

        String token = generateVerificationToken();
        saveVerificationToken(user, token, VerificationToken.TokenType.PASSWORD_RESET);
        emailService.sendPasswordResetEmail(email, token);
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmation request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken token = tokenRepository.findByTokenAndTokenType(
                        request.token(), VerificationToken.TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        validateTokenAndUser(token, user);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsed(true);

        userRepository.save(user);
        tokenRepository.save(token);
    }

    private void validateTokenAndUser(VerificationToken token, User user) {
        if (!token.getUser().getUserId().equals(user.getUserId())) {  // Changed from getUserId to getId
            throw new InvalidTokenException("Token does not belong to this user");
        }

        if (token.isExpired()) {
            throw new InvalidTokenException("Token has expired");
        }

        if (token.isUsed()) {
            throw new InvalidTokenException("Token has already been used");
        }
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    private String generateSmsCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private void saveVerificationToken(User user, String token, VerificationToken.TokenType type) {
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(user)
                .tokenType(type)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);
    }

    // Optional utility methods
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting scheduled cleanup of expired tokens");
        List<VerificationToken> expiredTokens = tokenRepository
                .findByExpiryDateBeforeAndUsed(LocalDateTime.now(), false);
        tokenRepository.deleteAll(expiredTokens);
        log.info("Completed cleanup of {} expired tokens", expiredTokens.size());
    }

    public boolean isEmailVerified(User user) {  // Changed to accept User instead of userId
        return tokenRepository
                .findByUserAndTokenType(user, VerificationToken.TokenType.EMAIL_VERIFICATION)
                .map(VerificationToken::isUsed)
                .orElse(false);
    }

    public VerificationToken validateToken(String token) {
        return tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));
    }

    public List<VerificationToken> getUserActiveTokens(User user) {  // Changed to accept User instead of userId
        return Arrays.stream(VerificationToken.TokenType.values())
                .map(tokenType -> tokenRepository.findByUserAndTokenType(user, tokenType))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(token -> !token.isExpired() && !token.isUsed())
                .collect(Collectors.toList());
    }
}
