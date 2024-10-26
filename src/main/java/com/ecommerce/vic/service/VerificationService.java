package com.ecommerce.vic.service;

import com.ecommerce.vic.dto.auth.PasswordResetConfirmation;
import com.ecommerce.vic.exception.InvalidTokenException;
import com.ecommerce.vic.model.User;
import com.ecommerce.vic.model.VerificationToken;
import com.ecommerce.vic.repository.UserRepository;
import com.ecommerce.vic.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Random;
import com.ecommerce.vic.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;

    public void sendEmailVerification(User user) {
        String token = generateVerificationToken();
        saveVerificationToken(user, token, VerificationToken.TokenType.EMAIL_VERIFICATION);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    public void sendSmsVerification(User user) {
        String code = generateSmsCode();
        saveVerificationToken(user, code, VerificationToken.TokenType.EMAIL_VERIFICATION);
        smsService.sendVerificationSms(user.getPhoneNumber(), code);
    }

    @Transactional
    public void verifyEmail(String email, String token) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        VerificationToken verificationToken = tokenRepository.findByTokenAndTokenType(
                token, VerificationToken.TokenType.EMAIL_VERIFICATION)
            .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Token has expired");
        }

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Token has already been used");
        }

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

        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Code has expired");
        }

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Code has already been used");
        }

        user.setPhoneVerified(true);
        verificationToken.setUsed(true);
        
        userRepository.save(user);
        tokenRepository.save(verificationToken);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

        if (token.isExpired()) {
            throw new InvalidTokenException("Reset token has expired");
        }

        if (token.isUsed()) {
            throw new InvalidTokenException("Reset token has already been used");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        token.setUsed(true);
        
        userRepository.save(user);
        tokenRepository.save(token);
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
}
