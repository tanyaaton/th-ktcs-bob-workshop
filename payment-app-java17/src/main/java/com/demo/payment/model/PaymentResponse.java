package com.demo.payment.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment response record - Java 17 modernized version
 * Replaces verbose builder pattern with concise record syntax
 * Includes static factory methods for common response patterns
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
     * Factory method for successful payment response
     */
    public static PaymentResponse success(Transaction transaction, String message) {
        return new PaymentResponse(
            transaction.getId(),
            transaction.getStatus(),
            transaction.getAuthorizationCode(),
            transaction.getAmount(),
            transaction.getCurrency(),
            transaction.getCardNumber(),
            message,
            LocalDateTime.now()
        );
    }
    
    /**
     * Factory method for error payment response
     */
    public static PaymentResponse error(String transactionId, String message) {
        return new PaymentResponse(
            transactionId,
            TransactionStatus.DECLINED,
            null,
            null,
            null,
            null,
            message,
            LocalDateTime.now()
        );
    }
}

// Made with Bob
