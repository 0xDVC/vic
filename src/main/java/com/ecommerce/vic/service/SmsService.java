package com.ecommerce.vic.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SmsService {
    // Implement with your preferred SMS provider (Twilio, AWS SNS, etc.)
    public void sendVerificationSms(String phoneNumber, String code) {
        // Implementation depends on your SMS provider
        // Example using Twilio would go here
        System.out.println("SMS verification code " + code + " sent to " + phoneNumber);
    }
}