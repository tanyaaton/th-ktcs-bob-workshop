package com.demo.payment.service;

import com.demo.payment.model.*;
import com.demo.payment.repository.TransactionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final Random random = new Random();

    // Test card numbers that always approve
    private static final List<String> TEST_CARDS = List.of(
            "4263970000005262",  // Visa
            "5425230000004415",  // MasterCard
            "374101000000608"    // Amex
    );

    public PaymentService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public PaymentResponse authorize(PaymentRequest request) {
        // Simulate processing delay (200-500ms)
        simulateProcessingDelay();

        // Mask card number (store only last 4 digits)
        String maskedCardNumber = maskCardNumber(request.cardNumber());

        // Determine if transaction should be approved
        boolean isApproved = shouldApprove(request.cardNumber());
        TransactionStatus status;
        String message;

        if (isApproved) {
            status = TransactionStatus.AUTHORIZED;
            message = "Transaction authorized successfully";
        } else {
            // Randomly assign decline reason
            status = getDeclineReason();
            message = getDeclineMessage(status);
        }

        // Create and save transaction
        Transaction transaction = Transaction.builder()
                .cardNumber(maskedCardNumber)
                .amount(request.amount())
                .currency(request.currency())
                .status(status)
                .authorizationCode(isApproved ? generateAuthCode() : null)
                .build();

        transaction = transactionRepository.save(transaction);

        // Build response using static factory method
        return PaymentResponse.success(transaction, message);
    }

    @Transactional
    public PaymentResponse capture(PaymentRequest request) {
        // Simulate processing delay
        simulateProcessingDelay();

        Optional<Transaction> optionalTransaction = transactionRepository.findById(request.transactionId());

        if (optionalTransaction.isEmpty()) {
            return PaymentResponse.error(request.transactionId(), "Transaction not found");
        }

        Transaction transaction = optionalTransaction.get();

        if (transaction.getStatus() != TransactionStatus.AUTHORIZED) {
            return PaymentResponse.error(
                    transaction.getId(),
                    "Transaction cannot be captured. Current status: " + transaction.getStatus()
            );
        }

        // Update transaction status to CAPTURED
        transaction.setStatus(TransactionStatus.CAPTURED);
        transaction = transactionRepository.save(transaction);

        return PaymentResponse.success(transaction, "Transaction captured successfully");
    }

    @Transactional
    public PaymentResponse refund(PaymentRequest request) {
        // Simulate processing delay
        simulateProcessingDelay();

        Optional<Transaction> optionalTransaction = transactionRepository.findById(request.transactionId());

        if (optionalTransaction.isEmpty()) {
            return PaymentResponse.error(request.transactionId(), "Transaction not found");
        }

        Transaction transaction = optionalTransaction.get();

        if (transaction.getStatus() != TransactionStatus.CAPTURED) {
            return PaymentResponse.error(
                    transaction.getId(),
                    "Transaction cannot be refunded. Current status: " + transaction.getStatus()
            );
        }

        // Update transaction status to REFUNDED
        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction = transactionRepository.save(transaction);

        return PaymentResponse.success(transaction, "Transaction refunded successfully");
    }

    @Cacheable(value = "transactions", key = "#transactionId")
    public Optional<Transaction> getTransaction(String transactionId) {
        return transactionRepository.findById(transactionId);
    }

    public List<Transaction> getRecentTransactions() {
        return transactionRepository.findTop50ByOrderByCreatedAtDesc();
    }

    // Helper methods

    private void simulateProcessingDelay() {
        try {
            // Random delay between 200-500ms
            int delay = 200 + random.nextInt(301);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    private boolean shouldApprove(String cardNumber) {
        // Test cards always approve
        if (TEST_CARDS.contains(cardNumber)) {
            return true;
        }

        // 10% random decline rate
        return random.nextDouble() > 0.10;
    }

    private TransactionStatus getDeclineReason() {
        double rand = random.nextDouble();

        if (rand < 0.05) {
            return TransactionStatus.INSUFFICIENT_FUNDS;
        } else if (rand < 0.10) {
            return TransactionStatus.EXPIRED_CARD;
        } else {
            return TransactionStatus.DECLINED;
        }
    }

    // Java 17: Enhanced switch expression with arrow syntax
    private String getDeclineMessage(TransactionStatus status) {
        return switch (status) {
            case INSUFFICIENT_FUNDS -> "Transaction declined: Insufficient funds";
            case EXPIRED_CARD -> "Transaction declined: Card expired";
            case DECLINED -> "Transaction declined";
            default -> "Transaction declined";
        };
    }

    private String generateAuthCode() {
        // Generate 6-digit authorization code
        return String.format("%06d", random.nextInt(1000000));
    }
}

// Modernized with Java 17

// Made with Bob
