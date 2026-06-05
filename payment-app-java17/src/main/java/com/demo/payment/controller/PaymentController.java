package com.demo.payment.controller;

import com.demo.payment.model.PaymentRequest;
import com.demo.payment.model.PaymentResponse;
import com.demo.payment.model.Transaction;
import com.demo.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/authorize")
    public ResponseEntity<PaymentResponse> authorize(@Valid @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.authorize(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.error(null, "Authorization failed: " + e.getMessage()));
        }
    }

    @PostMapping("/capture")
    public ResponseEntity<PaymentResponse> capture(@Valid @RequestBody PaymentRequest request) {
        try {
            if (request.transactionId() == null || request.transactionId().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(PaymentResponse.error(null, "Transaction ID is required"));
            }

            PaymentResponse response = paymentService.capture(request);

            if (response.message().contains("not found") || response.message().contains("cannot be captured")) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.error(null, "Capture failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<PaymentResponse> refund(@Valid @RequestBody PaymentRequest request) {
        try {
            if (request.transactionId() == null || request.transactionId().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(PaymentResponse.error(null, "Transaction ID is required"));
            }

            PaymentResponse response = paymentService.refund(request);

            if (response.message().contains("not found") || response.message().contains("cannot be refunded")) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.error(null, "Refund failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable String id) {
        try {
            Optional<Transaction> transaction = paymentService.getTransaction(id);

            if (transaction.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(PaymentResponse.error(id, "Transaction not found"));
            }

            return ResponseEntity.ok(transaction.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.error(id, "Failed to retrieve transaction: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        try {
            List<Transaction> transactions = paymentService.getRecentTransactions();
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentResponse.error(null, "Failed to retrieve transaction history: " + e.getMessage()));
        }
    }
}

// Modernized with Java 17

// Made with Bob
