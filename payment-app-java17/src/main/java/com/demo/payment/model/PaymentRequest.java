package com.demo.payment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Payment request record - Java 17 modernized version
 * Replaces verbose builder pattern with concise record syntax
 * Migrated from javax.validation to jakarta.validation
 */
public record PaymentRequest(
    @NotBlank(message = "Card number is required")
    String cardNumber,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    @NotBlank(message = "Currency is required")
    String currency,
    
    String cvv,
    String expiryMonth,
    String expiryYear,
    String transactionId
) {
    // Compact constructor for additional validation if needed
    public PaymentRequest {
        // Custom validation logic can be added here
        // Records automatically generate constructor, getters, equals, hashCode, toString
    }
}

// Made with Bob
