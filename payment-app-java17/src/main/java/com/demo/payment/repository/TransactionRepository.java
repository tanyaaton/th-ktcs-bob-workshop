package com.demo.payment.repository;

import com.demo.payment.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Transaction Repository - Java 17 Modernization
 * 
 * Migration Notes:
 * - No changes required - Spring Data JPA interface
 * - No javax.* dependencies (Spring Data handles JPA internally)
 * - JPQL query remains the same
 * 
 * This repository provides data access for Transaction entities
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC")
    List<Transaction> findTop50ByOrderByCreatedAtDesc();
}

// Made with Bob - Java 17 Modernization