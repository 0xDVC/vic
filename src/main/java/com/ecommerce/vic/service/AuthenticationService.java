package com.ecommerce.vic.service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.vic.constants.UserRole;
import com.ecommerce.vic.dto.auth.AdminInviteRequest;
import com.ecommerce.vic.dto.auth.AuthenticationRequest;
import com.ecommerce.vic.dto.auth.AuthenticationResponse;
import com.ecommerce.vic.dto.auth.RegisterRequest;
import com.ecommerce.vic.exception.CustomDisabledException;
import com.ecommerce.vic.exception.EmailAlreadyExistsException;
import com.ecommerce.vic.exception.UnauthorizedException;
import com.ecommerce.vic.model.User;
import com.ecommerce.vic.model.VerificationToken;
import com.ecommerce.vic.repository.UserRepository;
import com.ecommerce.vic.repository.VerificationTokenRepository;
import com.ecommerce.vic.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final VerificationTokenRepository tokenRepository;
    private final VerificationService verificationService;
    private final EmailService emailService;
    private final UserService userService;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Attempting to register new user with email: {}", request.email());
        
        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Registration failed - email already exists: {}", request.email());
            throw new EmailAlreadyExistsException("Email already registered");
        }

        var user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .streetAddress(request.streetAddress())
                .city(request.city())
                .state(request.state())
                .postalCode(request.postalCode())
                .country(request.country())
                .role(UserRole.CUSTOMER)
                .emailVerified(false)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        verificationService.sendEmailVerification(user);

        log.info("Successfully registered new user with email: {}", user.getEmail());
        
        if (request.phone() != null) {
            log.debug("Sending SMS verification for user: {}", user.getEmail());
            verificationService.sendSmsVerification(user);
        }

        String jwt = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwt)
                .requiresVerification(true)
                .isVerified(false)
                .message("Please verify your account")
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        log.info("Authentication attempt for user: {}", request.email());
        
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isEmailVerified()) {
            log.warn("Authentication failed - email not verified for user: {}", request.email());
            verificationService.sendEmailVerification(user);
            return AuthenticationResponse.builder()
                    .requiresVerification(true)
                    .isVerified(false)
                    .message("Please verify your email before logging in")
                    .build();
        }

        if (!user.isEnabled()) {
            log.warn("Authentication failed - account disabled for user: {}", request.email());
            throw new CustomDisabledException("Account is disabled");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Generate token only if all checks pass
        String jwt = jwtService.generateToken(user);

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Successfully authenticated user: {}", request.email());
        return AuthenticationResponse.builder()
                .token(jwt)
                .isVerified(true)
                .message("Login successful")
                .build();
    }

    @Transactional
    public void createAdmin(AdminInviteRequest request) {
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Only admins can create new admins");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already registered");
        }

        String token = UUID.randomUUID().toString();
        String temporaryPassword = generateTemporaryPassword();

        var admin = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(UserRole.ADMIN)
                .emailVerified(false)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(admin);
        
        saveAdminInvitationToken(admin, token);
        
        emailService.sendAdminInvitation(admin.getEmail(), token, temporaryPassword);
    }

    private void saveAdminInvitationToken(User admin, String token) {
        VerificationToken verificationToken = VerificationToken.builder()
                .token(token)
                .user(admin)
                .tokenType(VerificationToken.TokenType.ADMIN_INVITATION)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
