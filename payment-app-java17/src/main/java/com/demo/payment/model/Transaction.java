package com.demo.payment.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Entity - Java 17 Modernization
 * 
 * Migration Notes:
 * - Migrated from javax.persistence to jakarta.persistence (Spring Boot 3.x requirement)
 * - Kept as traditional class (not record) because JPA entities require mutability
 * - JPA needs default constructor and setters for entity management
 * - Retained builder pattern for convenient object creation
 * 
 * Why not a Record?
 * - JPA requires mutable entities for lazy loading and proxy creation
 * - @PrePersist and @PreUpdate callbacks need to modify entity state
 * - Hibernate needs to set field values during entity hydration
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "card_number", nullable = false)
    private String cardNumber;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback - executed before entity is persisted
     * Automatically generates UUID and sets timestamps
     */
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback - executed before entity is updated
     * Automatically updates the updatedAt timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Default constructor required by JPA
    public Transaction() {
    }

    // Builder pattern for convenient object creation
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Transaction transaction = new Transaction();

        public Builder cardNumber(String cardNumber) {
            transaction.cardNumber = cardNumber;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            transaction.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            transaction.currency = currency;
            return this;
        }

        public Builder status(TransactionStatus status) {
            transaction.status = status;
            return this;
        }

        public Builder authorizationCode(String authorizationCode) {
            transaction.authorizationCode = authorizationCode;
            return this;
        }

        public Transaction build() {
            return transaction;
        }
    }

    // Getters and Setters (required by JPA)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

// Made with Bob - Java 17 Modernization