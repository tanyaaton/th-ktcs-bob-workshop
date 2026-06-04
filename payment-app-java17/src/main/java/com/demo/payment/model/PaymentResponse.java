package com.demo.payment.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment Response Record - Java 17 Modernization
 * 
 * Converted from traditional POJO (140 lines) to Java Record (95 lines)
 * Benefits:
 * - Immutable by default
 * - Automatic equals(), hashCode(), toString()
 * - No boilerplate getters/setters
 * - Thread-safe
 * - Clear data contract
 * 
 * Migration Notes:
 * - Removed builder pattern (use factory methods instead)
 * - All fields are final and immutable
 * - Factory methods provide flexible construction options
 */
public record PaymentResponse(
    String transactionId,
    TransactionStatus status,
    String authorizationCode,
    BigDecimal amount,
    String currency,
    String cardNumberMasked,
    String message,
    LocalDateTime timestamp
) {
    /**
     * Compact constructor for validation and normalization
     */
    public PaymentResponse {
        // Ensure timestamp is set if not provided
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        
        // Normalize currency to uppercase if provided
        if (currency != null) {
            currency = currency.toUpperCase();
        }
    }
    
    /**
     * Factory method for successful payment response
     */
    public static PaymentResponse success(
            String transactionId,
            String authorizationCode,
            BigDecimal amount,
            String currency,
            String cardNumberMasked) {
        return new PaymentResponse(
            transactionId,
            TransactionStatus.APPROVED,
            authorizationCode,
            amount,
            currency,
            cardNumberMasked,
            "Payment processed successfully",
            LocalDateTime.now()
        );
    }
    
    /**
     * Factory method for declined payment response
     */
    public static PaymentResponse declined(
            String transactionId,
            TransactionStatus status,
            BigDecimal amount,
            String currency,
            String cardNumberMasked,
            String reason) {
        return new PaymentResponse(
            transactionId,
            status,
            null, // No authorization code for declined payments
            amount,
            currency,
            cardNumberMasked,
            reason,
            LocalDateTime.now()
        );
    }
    
    /**
     * Factory method for error response
     */
    public static PaymentResponse error(String transactionId, String errorMessage) {
        return new PaymentResponse(
            transactionId,
            TransactionStatus.DECLINED,
            null,
            null,
            null,
            null,
            errorMessage,
            LocalDateTime.now()
        );
    }
    
    /**
     * Factory method for authorized payment (not yet captured)
     */
    public static PaymentResponse authorized(
            String transactionId,
            String authorizationCode,
            BigDecimal amount,
            String currency,
            String cardNumberMasked) {
        return new PaymentResponse(
            transactionId,
            TransactionStatus.AUTHORIZED,
            authorizationCode,
            amount,
            currency,
            cardNumberMasked,
            "Payment authorized successfully",
            LocalDateTime.now()
        );
    }
    
    /**
     * Check if the payment was successful
     */
    public boolean isSuccessful() {
        return status == TransactionStatus.APPROVED || 
               status == TransactionStatus.AUTHORIZED ||
               status == TransactionStatus.CAPTURED;
    }
    
    /**
     * Check if the payment was declined
     */
    public boolean isDeclined() {
        return status == TransactionStatus.DECLINED ||
               status == TransactionStatus.INSUFFICIENT_FUNDS ||
               status == TransactionStatus.EXPIRED_CARD;
    }
    
    /**
     * Create a copy with updated status
     * Records are immutable, so we create a new instance
     */
    public PaymentResponse withStatus(TransactionStatus newStatus, String newMessage) {
        return new PaymentResponse(
            this.transactionId,
            newStatus,
            this.authorizationCode,
            this.amount,
            this.currency,
            this.cardNumberMasked,
            newMessage,
            LocalDateTime.now()
        );
    }
}

// Made with Bob - Java 17 Modernization