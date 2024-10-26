package com.ecommerce.vic.controller;

import com.ecommerce.vic.dto.auth.RegisterRequest;
import com.ecommerce.vic.dto.auth.AuthenticationRequest;
import com.ecommerce.vic.dto.auth.AuthenticationResponse;
import com.ecommerce.vic.dto.auth.VerificationRequest;
import com.ecommerce.vic.dto.auth.PasswordResetRequest;
import com.ecommerce.vic.dto.auth.PasswordResetConfirmation;
import com.ecommerce.vic.dto.auth.AdminInviteRequest;
import com.ecommerce.vic.service.AuthenticationService;
import com.ecommerce.vic.service.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final VerificationService verificationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody @Valid AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/verify/email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerificationRequest request) {
        verificationService.verifyEmail(request.email(), request.code());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify/phone")
    public ResponseEntity<Void> verifyPhone(@Valid @RequestBody VerificationRequest request) {
        verificationService.verifyPhone(request.email(), request.code());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        verificationService.initiatePasswordReset(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmation request) {
        verificationService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> inviteAdmin(@Valid @RequestBody AdminInviteRequest request) {
        authenticationService.createAdmin(request);
        return ResponseEntity.ok().build();
    }
}
