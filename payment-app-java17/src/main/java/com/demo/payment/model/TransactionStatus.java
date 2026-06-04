package com.demo.payment.model;

/**
 * Transaction Status Enum - Java 17 Modernization
 * 
 * No changes required - enums are already immutable and type-safe
 * This enum represents all possible states of a payment transaction
 */
public enum TransactionStatus {
    AUTHORIZED,
    CAPTURED,
    REFUNDED,
    DECLINED,
    APPROVED,
    INSUFFICIENT_FUNDS,
    EXPIRED_CARD
}

// Made with Bob - Java 17 Modernization