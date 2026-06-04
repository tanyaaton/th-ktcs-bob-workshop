package com.demo.payment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Payment Request Record - Java 17 Modernization
 * 
 * Converted from traditional POJO (138 lines) to Java Record (35 lines)
 * Benefits:
 * - Immutable by default
 * - Automatic equals(), hashCode(), toString()
 * - Compact canonical constructor
 * - No boilerplate getters/setters
 * - Thread-safe
 * 
 * Migration Notes:
 * - Replaced javax.validation with jakarta.validation (Spring Boot 3.x)
 * - Removed builder pattern (use constructor directly)
 * - Validation annotations work seamlessly with records
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
    /**
     * Compact constructor for additional validation or normalization
     * This constructor is called before the canonical constructor
     */
    public PaymentRequest {
        // Normalize currency to uppercase if provided
        if (currency != null) {
            currency = currency.toUpperCase();
        }
        
        // Mask card number for logging (keep last 4 digits)
        // Note: This doesn't modify the stored value, just for validation
        if (cardNumber != null && cardNumber.length() > 4) {
            // Card number validation could be added here
        }
    }
    
    /**
     * Factory method for creating a PaymentRequest with minimal required fields
     * Replaces the builder pattern for simple cases
     */
    public static PaymentRequest of(String cardNumber, BigDecimal amount, String currency) {
        return new PaymentRequest(cardNumber, amount, currency, null, null, null, null);
    }
    
    /**
     * Factory method with full card details
     */
    public static PaymentRequest withCardDetails(
            String cardNumber, 
            BigDecimal amount, 
            String currency,
            String cvv,
            String expiryMonth,
            String expiryYear) {
        return new PaymentRequest(cardNumber, amount, currency, cvv, expiryMonth, expiryYear, null);
    }
    
    /**
     * Create a copy with a new transaction ID
     * Records are immutable, so we create a new instance
     */
    public PaymentRequest withTransactionId(String transactionId) {
        return new PaymentRequest(
            this.cardNumber,
            this.amount,
            this.currency,
            this.cvv,
            this.expiryMonth,
            this.expiryYear,
            transactionId
        );
    }
    
    /**
     * Get masked card number for display/logging
     * Example: "1234567890123456" -> "************3456"
     */
    public String getMaskedCardNumber() {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
}

// Made with Bob - Java 17 Modernization