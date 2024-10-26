package com.ecommerce.vic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final String fromEmail = "noreply@yourdomain.com";

    public void sendVerificationEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Email Verification");
        message.setText("Please verify your email by entering this code: " + token);
        mailSender.send(message);
    }

    public void sendAdminInvitation(String toEmail, String token, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Admin Account Invitation");
        message.setText("You have been invited as an admin. Your temporary password is: " + temporaryPassword +
                "\nPlease use this verification code to complete setup: " + token);
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        message.setText("Your password reset code is: " + token);
        mailSender.send(message);
    }
}
