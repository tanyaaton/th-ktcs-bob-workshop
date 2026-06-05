# Java 11 to Java 17 Modernization Plan
## Payment Processing Application

**Document Version:** 1.0  
**Date:** June 5, 2026  
**Current Version:** Java 11 with Spring Boot 2.7.18  
**Target Version:** Java 17 with Spring Boot 3.x

---

## Executive Summary

This document outlines the comprehensive plan to modernize the Payment Processing Application from Java 11 to Java 17. The migration includes upgrading Spring Boot from 2.7.18 to 3.2.x (latest stable), replacing deprecated APIs, and leveraging new Java 17 features to improve code quality, performance, and maintainability.

**Estimated Total Effort:** 36-54 hours  
**Overall Risk Level:** Medium  
**Recommended Timeline:** 2-3 weeks (including testing)

---

## Table of Contents

1. [Current State Analysis](#1-current-state-analysis)
2. [Summary of Changes Required](#2-summary-of-changes-required)
3. [Deprecated and Removed APIs](#3-deprecated-and-removed-apis)
4. [Java 17 Feature Opportunities](#4-java-17-feature-opportunities)
5. [Dependency Updates](#5-dependency-updates)
6. [Configuration Changes](#6-configuration-changes)
7. [Implementation Roadmap](#7-implementation-roadmap)
8. [Risk Assessment](#8-risk-assessment)
9. [Testing Strategy](#9-testing-strategy)
10. [Rollback Plan](#10-rollback-plan)

---

## 1. Current State Analysis

### 1.1 Application Overview
- **Type:** Spring Boot REST API for payment processing
- **Current Java Version:** 11
- **Current Spring Boot Version:** 2.7.18
- **Build Tool:** Maven 3.8
- **Database:** H2 (in-memory)
- **Key Dependencies:**
  - Spring Boot Starter Web
  - Spring Boot Starter Data JPA
  - Spring Boot Starter Cache (Caffeine)
  - Spring Boot Actuator
  - Micrometer Prometheus
  - Bean Validation (javax.validation)

### 1.2 Code Structure
```
payment-app/
├── model/
│   ├── PaymentRequest.java (Builder pattern, javax.validation)
│   ├── PaymentResponse.java (Builder pattern)
│   ├── Transaction.java (JPA Entity, Builder pattern, javax.persistence)
│   └── TransactionStatus.java (Enum)
├── service/
│   ├── PaymentService.java (Business logic, Optional usage)
│   └── CacheService.java
├── controller/
│   ├── PaymentController.java (REST endpoints, javax.validation)
│   └── AdminController.java
├── repository/
│   └── TransactionRepository.java (Spring Data JPA)
└── config/
    └── CacheConfig.java (Caffeine configuration)
```

### 1.3 Key Findings
- ✅ No usage of removed Java APIs (e.g., Nashorn, Pack200)
- ⚠️ Uses `javax.*` packages (need migration to `jakarta.*`)
- ⚠️ Manual Builder pattern implementations (can be replaced with records)
- ⚠️ Traditional switch statements (can use enhanced switch expressions)
- ⚠️ Optional.isPresent() pattern (can use modern alternatives)
- ✅ Already uses LocalDateTime (no Date/Calendar issues)
- ⚠️ Dockerfile uses Java 11 base images

---

## 2. Summary of Changes Required

### 2.1 Critical Changes (Must Do)
1. **Java Version Update**
   - Update `java.version` from 11 to 17 in pom.xml
   - Update Maven compiler plugin source/target to 17
   - Update Dockerfile base images to Java 17

2. **Spring Boot Major Version Upgrade**
   - Upgrade from Spring Boot 2.7.18 to 3.2.x
   - This is a major version change with breaking changes

3. **Namespace Migration (javax → jakarta)**
   - Replace all `javax.persistence.*` with `jakarta.persistence.*`
   - Replace all `javax.validation.*` with `jakarta.validation.*`
   - Update imports across all affected files

4. **Dependency Updates**
   - Update all Spring Boot dependencies to 3.2.x versions
   - Update Maven compiler plugin to 3.11.0+
   - Verify H2 database compatibility

### 2.2 Recommended Changes (Should Do)
1. **Leverage Java 17 Records**
   - Convert DTOs to records (PaymentRequest, PaymentResponse)
   - Simplify immutable data classes

2. **Enhanced Switch Expressions**
   - Modernize switch statements in PaymentService
   - Use pattern matching where applicable

3. **Sealed Classes**
   - Consider sealed hierarchy for TransactionStatus-related types

4. **Text Blocks**
   - Use for multi-line strings (SQL queries, JSON templates)

5. **Pattern Matching**
   - Replace instanceof checks with pattern matching
   - Use pattern matching for Optional

### 2.3 Optional Enhancements (Nice to Have)
1. **NullPointerException improvements**
   - Leverage helpful NPE messages (Java 14+)

2. **Stream API enhancements**
   - Use new Stream methods (toList(), etc.)

3. **Performance optimizations**
   - Leverage JVM improvements in Java 17

---

## 3. Deprecated and Removed APIs

### 3.1 APIs Requiring Immediate Replacement

#### 3.1.1 javax.* → jakarta.* Migration

**Impact:** HIGH - Affects multiple files  
**Effort:** 2-3 hours  
**Risk:** LOW (straightforward find-replace)

| Current Package | New Package | Affected Files |
|----------------|-------------|----------------|
| `javax.persistence.*` | `jakarta.persistence.*` | Transaction.java |
| `javax.validation.*` | `jakarta.validation.*` | PaymentRequest.java, PaymentController.java |

**Files to Update:**
1. **Transaction.java** (Line 3)
   ```java
   // OLD
   import javax.persistence.*;
   
   // NEW
   import jakarta.persistence.*;
   ```

2. **PaymentRequest.java** (Lines 3-5)
   ```java
   // OLD
   import javax.validation.constraints.NotBlank;
   import javax.validation.constraints.NotNull;
   import javax.validation.constraints.Positive;
   
   // NEW
   import jakarta.validation.constraints.NotBlank;
   import jakarta.validation.constraints.NotNull;
   import jakarta.validation.constraints.Positive;
   ```

3. **PaymentController.java** (Line 11)
   ```java
   // OLD
   import javax.validation.Valid;
   
   // NEW
   import jakarta.validation.Valid;
   ```

### 3.2 Spring Boot 2.x → 3.x Breaking Changes

**Impact:** HIGH  
**Effort:** 4-6 hours  
**Risk:** MEDIUM

#### 3.2.1 Configuration Properties
- Spring Boot 3.x has stricter configuration binding
- Review `application.properties` for deprecated properties

#### 3.2.2 Actuator Endpoints
- Some actuator endpoint paths may have changed
- Verify Prometheus metrics endpoint compatibility

#### 3.2.3 Security (if added later)
- Spring Security 6.x has significant changes
- Currently not used, but document for future reference

### 3.3 No Deprecated APIs Found

The following Java 11 deprecated APIs are **NOT** used in this codebase:
- ✅ Nashorn JavaScript engine (removed in Java 15)
- ✅ Pack200 tools (removed in Java 14)
- ✅ RMI Activation (removed in Java 17)
- ✅ Applet API (deprecated for removal)
- ✅ Security Manager (deprecated in Java 17)

---

## 4. Java 17 Feature Opportunities

### 4.1 Records (JEP 395 - Java 16)

**Impact:** HIGH - Significant code reduction  
**Effort:** 3-4 hours  
**Risk:** LOW  
**Benefits:** Immutability, less boilerplate, better semantics

#### 4.1.1 Convert PaymentRequest to Record

**Current Implementation:** 138 lines with manual builder
```java
public class PaymentRequest {
    private String cardNumber;
    private BigDecimal amount;
    // ... 130+ lines of getters, setters, builder
}
```

**Proposed Record Implementation:** ~20 lines
```java
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
    // Compact constructor for validation if needed
    public PaymentRequest {
        // Custom validation logic here
    }
}
```

**Benefits:**
- Reduces code by ~85%
- Automatic equals(), hashCode(), toString()
- Immutable by default
- Clear intent as data carrier

#### 4.1.2 Convert PaymentResponse to Record

**Current:** 140 lines  
**Proposed:** ~15 lines with factory methods

```java
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
    
    public static PaymentResponse error(String transactionId, String message) {
        return new PaymentResponse(
            transactionId,
            TransactionStatus.DECLINED,
            null, null, null, null,
            message,
            LocalDateTime.now()
        );
    }
}
```

**Note:** Transaction.java should remain a JPA entity class (not converted to record) as JPA requires mutable entities.

### 4.2 Enhanced Switch Expressions (JEP 361 - Java 14)

**Impact:** MEDIUM  
**Effort:** 1-2 hours  
**Risk:** LOW  
**Benefits:** More concise, expression-based, exhaustiveness checking

#### 4.2.1 Modernize getDeclineMessage()

**Current Implementation (PaymentService.java, lines 200-210):**
```java
private String getDeclineMessage(TransactionStatus status) {
    switch (status) {
        case INSUFFICIENT_FUNDS:
            return "Transaction declined: Insufficient funds";
        case EXPIRED_CARD:
            return "Transaction declined: Card expired";
        case DECLINED:
        default:
            return "Transaction declined";
    }
}
```

**Proposed Switch Expression:**
```java
private String getDeclineMessage(TransactionStatus status) {
    return switch (status) {
        case INSUFFICIENT_FUNDS -> "Transaction declined: Insufficient funds";
        case EXPIRED_CARD -> "Transaction declined: Card expired";
        case DECLINED -> "Transaction declined";
        default -> "Transaction declined";
    };
}
```

**Benefits:**
- No break statements needed
- Returns value directly
- Compiler ensures exhaustiveness
- More concise and readable

### 4.3 Pattern Matching for instanceof (JEP 394 - Java 16)

**Impact:** LOW (limited usage in current code)  
**Effort:** 30 minutes  
**Risk:** LOW

**Current Pattern:**
```java
if (obj instanceof String) {
    String str = (String) obj;
    // use str
}
```

**New Pattern:**
```java
if (obj instanceof String str) {
    // use str directly
}
```

**Note:** Limited applicability in current codebase, but good practice for future code.

### 4.4 Sealed Classes (JEP 409 - Java 17)

**Impact:** MEDIUM  
**Effort:** 2-3 hours  
**Risk:** LOW  
**Benefits:** Controlled inheritance, better domain modeling

#### 4.4.1 Create Sealed Transaction Status Hierarchy

**Current:** Simple enum (TransactionStatus.java)

**Proposed Enhancement:**
```java
public sealed interface TransactionResult 
    permits SuccessfulTransaction, FailedTransaction {
    TransactionStatus status();
    String message();
}

public record SuccessfulTransaction(
    TransactionStatus status,
    String authorizationCode,
    String message
) implements TransactionResult {}

public record FailedTransaction(
    TransactionStatus status,
    String message,
    FailureReason reason
) implements TransactionResult {}

public enum FailureReason {
    INSUFFICIENT_FUNDS,
    EXPIRED_CARD,
    INVALID_CARD,
    FRAUD_DETECTED
}
```

**Benefits:**
- Type-safe transaction results
- Compiler-enforced exhaustiveness
- Better domain modeling
- Clearer API contracts

**Note:** This is an optional enhancement that requires more significant refactoring.

### 4.5 Text Blocks (JEP 378 - Java 15)

**Impact:** LOW (limited usage)  
**Effort:** 30 minutes  
**Risk:** LOW

**Use Cases:**
- SQL queries (if added)
- JSON templates for testing
- Multi-line error messages

**Example:**
```java
// OLD
String json = "{\n" +
              "  \"status\": \"error\",\n" +
              "  \"message\": \"Transaction failed\"\n" +
              "}";

// NEW
String json = """
    {
      "status": "error",
      "message": "Transaction failed"
    }
    """;
```

### 4.6 Helpful NullPointerExceptions (JEP 358 - Java 14)

**Impact:** LOW (automatic)  
**Effort:** 0 hours  
**Risk:** NONE  
**Benefits:** Better debugging

**Automatic Improvement:**
```java
// OLD NPE message:
// java.lang.NullPointerException

// NEW NPE message (Java 14+):
// java.lang.NullPointerException: 
//   Cannot invoke "String.length()" because "transaction.getCardNumber()" is null
```

**Action Required:** None - automatic JVM improvement

### 4.7 Stream API Enhancements

**Impact:** LOW  
**Effort:** 1 hour  
**Risk:** LOW

#### 4.7.1 Stream.toList() (Java 16)

**Current:**
```java
List<Transaction> list = stream.collect(Collectors.toList());
```

**New:**
```java
List<Transaction> list = stream.toList();
```

**Benefits:**
- More concise
- Returns unmodifiable list
- Better performance

---

## 5. Dependency Updates

### 5.1 Maven POM Changes

**File:** `pom.xml`  
**Impact:** HIGH  
**Effort:** 2-3 hours  
**Risk:** MEDIUM

#### 5.1.1 Java Version Properties

```xml
<!-- CURRENT (Lines 21-26) -->
<properties>
    <java.version>11</java.version>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<!-- UPDATED -->
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

#### 5.1.2 Spring Boot Parent Version

```xml
<!-- CURRENT (Lines 8-13) -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
    <relativePath/>
</parent>

<!-- UPDATED -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version> <!-- Latest stable 3.x -->
    <relativePath/>
</parent>
```

**Note:** Spring Boot 3.2.5 is the latest stable version as of this plan. Check for newer versions at implementation time.

#### 5.1.3 Maven Compiler Plugin

```xml
<!-- CURRENT (Lines 92-100) -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.8.1</version>
    <configuration>
        <source>11</source>
        <target>11</target>
    </configuration>
</plugin>

<!-- UPDATED -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.11.0</version>
    <configuration>
        <source>17</source>
        <target>17</target>
        <release>17</release> <!-- Recommended for Java 9+ -->
    </configuration>
</plugin>
```

#### 5.1.4 Dependency Version Management

All Spring Boot dependencies will be automatically updated via the parent POM. However, verify compatibility:

| Dependency | Current (Boot 2.7.18) | Updated (Boot 3.2.5) | Notes |
|-----------|----------------------|---------------------|-------|
| Spring Web | 5.3.x | 6.1.x | Major version change |
| Spring Data JPA | 2.7.x | 3.2.x | Major version change |
| Hibernate | 5.6.x | 6.4.x | Major version change |
| H2 Database | 2.1.x | 2.2.x | Minor update |
| Caffeine | 3.1.x | 3.1.x | Compatible |
| Micrometer | 1.10.x | 1.12.x | Minor update |

**Action Items:**
1. Remove explicit version declarations (let parent manage)
2. Test H2 database compatibility
3. Verify Caffeine cache behavior
4. Check Micrometer Prometheus metrics

---

## 6. Configuration Changes

### 6.1 Dockerfile Updates

**File:** `Dockerfile`  
**Impact:** HIGH  
**Effort:** 1 hour  
**Risk:** LOW

#### 6.1.1 Build Stage

```dockerfile
# CURRENT (Line 11)
FROM maven:3.8-openjdk-11 AS builder

# UPDATED
FROM maven:3.9-eclipse-temurin-17 AS builder
```

**Rationale:**
- Maven 3.9 is the latest stable version
- Eclipse Temurin is the recommended OpenJDK distribution
- Java 17 LTS support

#### 6.1.2 Runtime Stage

```dockerfile
# CURRENT (Line 44)
FROM gcr.io/distroless/java11-debian11

# UPDATED
FROM gcr.io/distroless/java17-debian12
```

**Rationale:**
- Java 17 runtime
- Debian 12 (Bookworm) is the latest stable
- Maintains distroless security benefits

### 6.2 Application Properties

**File:** `application.properties`  
**Impact:** LOW  
**Effort:** 30 minutes  
**Risk:** LOW

**Action Items:**
1. Review for deprecated Spring Boot 2.x properties
2. Update actuator endpoint configurations if needed
3. Verify H2 console configuration (if used)
4. Check logging configuration compatibility

---

## 7. Implementation Roadmap

### Phase 1: Preparation (2-3 days)
**Effort:** 4-6 hours

1. Create feature branch: `feature/java-17-migration`
2. Tag current version: `v1.0.0-java11`
3. Set up Java 17 environment
4. Create comprehensive test suite
5. Review Spring Boot 3.x migration guide

### Phase 2: Core Migration (3-4 days)
**Effort:** 12-16 hours

1. Update pom.xml (Java 17, Spring Boot 3.2.x)
2. Update Dockerfile base images
3. Migrate javax → jakarta namespaces
4. Fix Spring Boot 3.x breaking changes
5. Integration testing

### Phase 3: Java 17 Feature Adoption (3-5 days)
**Effort:** 8-12 hours

1. Convert DTOs to records
2. Modernize switch statements
3. Apply pattern matching
4. Consider sealed classes (optional)

### Phase 4: Testing & Validation (2-3 days)
**Effort:** 8-12 hours

1. Unit testing
2. Integration testing
3. Performance testing
4. Docker testing
5. Documentation update

### Phase 5: Deployment & Monitoring (1-2 days)
**Effort:** 4-8 hours

1. Staging deployment
2. Production deployment
3. Monitoring and validation

**Total Estimated Effort:** 36-54 hours

---

## 8. Risk Assessment

### 8.1 Risk Matrix

| Risk | Probability | Impact | Severity | Mitigation |
|------|------------|--------|----------|------------|
| Spring Boot 3.x breaking changes | HIGH | HIGH | **CRITICAL** | Thorough testing, staged rollout |
| javax → jakarta migration issues | MEDIUM | MEDIUM | **HIGH** | Automated testing, careful review |
| Performance regression | LOW | MEDIUM | **MEDIUM** | Baseline metrics, load testing |
| Docker image issues | LOW | LOW | **LOW** | Test in staging first |
| Dependency conflicts | MEDIUM | MEDIUM | **MEDIUM** | Dependency analysis |

### 8.2 Critical Risks

#### Risk 1: Spring Boot 3.x Breaking Changes
**Mitigation:**
- Comprehensive testing in staging
- Review Spring Boot 3.x migration guide
- Staged rollout (canary deployment)
- Quick rollback capability

#### Risk 2: javax → jakarta Namespace Migration
**Mitigation:**
- Use IDE refactoring tools
- Automated search for javax imports
- Comprehensive compilation checks
- Unit and integration testing

---

## 9. Testing Strategy

### 9.1 Test Levels

#### Unit Tests
- Coverage Target: 80%+
- Tools: JUnit 5, Mockito
- Command: `mvn test`

#### Integration Tests
- API endpoints
- Database integration
- Cache integration
- Command: `mvn verify`

#### Performance Tests
- Load testing (100 concurrent users)
- Stress testing
- Endurance testing (2 hours)
- Tools: JMeter, Gatling

### 9.2 Test Data

**Test Cards:**
```
Visa:       4263970000005262 (Always approve)
MasterCard: 5425230000004415 (Always approve)
Amex:       374101000000608  (Always approve)
```

---

## 10. Rollback Plan

### 10.1 Rollback Triggers

**Immediate Rollback Required If:**
1. Application fails to start
2. Critical API endpoints non-functional
3. Data corruption detected
4. Performance degradation >50%
5. Error rate >5%

### 10.2 Rollback Procedure

**For Docker Deployment:**
```bash
# Stop current container
docker stop payment-app

# Start previous version
docker run -d --name payment-app \
  -p 8080:8080 \
  payment-app:v1.0.0-java11
```

**For Kubernetes:**
```bash
kubectl rollout undo deployment/payment-app
```

---

## Appendix: Quick Reference

### Key Commands

```bash
# Build
mvn clean package

# Test
mvn test
mvn verify

# Run
mvn spring-boot:run

# Docker
docker build -t payment-app:java17 .
docker run -p 8080:8080 payment-app:java17
```

### Important URLs

- Application: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus

### Effort Summary

| Phase | Effort (hours) | Risk |
|-------|----------------|------|
| Preparation | 4-6 | LOW |
| Core Migration | 12-16 | MEDIUM |
| Feature Adoption | 8-12 | LOW |
| Testing | 8-12 | MEDIUM |
| Deployment | 4-8 | MEDIUM |
| **TOTAL** | **36-54** | **MEDIUM** |

---

**End of Document**