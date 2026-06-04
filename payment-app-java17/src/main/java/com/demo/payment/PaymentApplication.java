package com.demo.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Payment Application - Java 17 Modernization
 * 
 * Migration Notes:
 * - No changes required - Spring Boot main class
 * - Compatible with Spring Boot 3.2.5 and Java 17
 * - No javax.* dependencies
 * 
 * This is the main entry point for the Payment Processing Application
 */
@SpringBootApplication
@EnableCaching
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}

// Made with Bob - Java 17 Modernization