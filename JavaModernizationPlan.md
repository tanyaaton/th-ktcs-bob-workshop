# Java 11 to Java 17 Modernization Plan
## Payment Processing Application

**Document Version:** 1.0  
**Date:** June 4, 2026  
**Application:** Payment Processing Application v1.0.0  
**Current Java Version:** 11  
**Target Java Version:** 17

---

## Executive Summary

This document outlines the comprehensive plan to modernize the Payment Processing Application from Java 11 to Java 17. The migration will leverage new language features, improve code quality, enhance performance, and ensure compatibility with the latest Spring Boot ecosystem.

**Key Benefits:**
- Enhanced performance with improved JVM optimizations
- Better code readability with modern Java features (Records, Sealed Classes, Pattern Matching)
- Improved security with latest JDK security patches
- Access to new APIs and language enhancements
- Long-term support (Java 17 is an LTS release)

---

## 1. Summary of Changes Required

### 1.1 Dependencies & Framework Updates

| Component | Current Version | Target Version | Impact |
|-----------|----------------|----------------|---------|
| Java | 11 | 17 | High |
| Spring Boot | 2.7.18 | 3.2.x | High |
| Maven Compiler Plugin | 3.8.1 | 3.11.0 | Low |
| H2 Database | (inherited) | Latest compatible | Medium |
| Caffeine Cache | (inherited) | Latest compatible | Low |
| Micrometer | (inherited) | Latest compatible | Low |

### 1.2 API & Syntax Changes

| Category | Changes Required | Files Affected |
|----------|------------------|----------------|
| javax.* → jakarta.* | Package namespace migration | 5 files |
| Optional.isPresent() | Replace with modern patterns | 2 files |
| Switch expressions | Modernize switch statements | 1 file |
| Text blocks | Improve SQL/JSON strings | 1 file |
| Records | Convert DTOs to records | 3 files |
| Sealed classes | Add type safety to enums | 1 file |
| Pattern matching | Simplify instanceof checks | 0 files (opportunity) |

### 1.3 Configuration Changes

| File | Changes Required |
|------|------------------|
| pom.xml | Update Java version, Spring Boot parent, dependencies |
| Dockerfile | Update base images to Java 17 |
| application.properties | Update deprecated properties (if any) |

---

## 2. Deprecated & Removed APIs

### 2.1 Critical: javax.* to jakarta.* Migration

**Impact:** HIGH - Breaking change in Spring Boot 3.x

**Affected Files:**
1. `PaymentRequest.java` - Lines 3-5
2. `PaymentController.java` - Line 11
3. `Transaction.java` - Line 3

**Changes Required:**
```java
// OLD (Java 11 + Spring Boot 2.x)
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.persistence.*;

// NEW (Java 17 + Spring Boot 3.x)
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.persistence.*;
```

**Files to Update:**
- `PaymentRequest.java`
- `PaymentController.java`
- `Transaction.java`

### 2.2 Optional.isPresent() Pattern

**Impact:** LOW - Not deprecated but can be modernized

**Current Usage:**
```java
// PaymentController.java - Line 84
if (!transaction.isPresent()) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(buildErrorResponse("Transaction not found"));
}
return ResponseEntity.ok(transaction.get());
```

**Modern Alternative:**
```java
return transaction
    .map(ResponseEntity::ok)
    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(buildErrorResponse("Transaction not found")));
```

### 2.3 Switch Statement Modernization

**Impact:** LOW - Enhancement opportunity

**Current Usage in PaymentService.java (Lines 200-209):**
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

**Modern Switch Expression:**
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

---

## 3. New Java 17 Features to Leverage

### 3.1 Records (JEP 395) - HIGH PRIORITY

**Benefits:** Immutable data carriers, automatic equals/hashCode/toString, reduced boilerplate

**Candidates for Conversion:**

#### 3.1.1 PaymentRequest → Record
**Current:** 138 lines with builder pattern  
**Estimated Reduction:** ~100 lines

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

**Impact:** 
- ✅ Eliminates 100+ lines of boilerplate
- ✅ Immutability by default
- ✅ Thread-safe
- ⚠️ Requires updating all builder pattern usage

#### 3.1.2 PaymentResponse → Record
**Current:** 140 lines with builder pattern  
**Estimated Reduction:** ~100 lines

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
    // Factory methods can replace builder pattern
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
            null,
            null,
            null,
            null,
            message,
            LocalDateTime.now()
        );
    }
}
```

#### 3.1.3 Keep Transaction as JPA Entity
**Decision:** Do NOT convert to record  
**Reason:** JPA entities require mutability for lazy loading and proxy creation

### 3.2 Sealed Classes (JEP 409) - MEDIUM PRIORITY

**Benefits:** Controlled inheritance, exhaustive pattern matching, better type safety

**Candidate: TransactionStatus Enum Enhancement**

```java
public sealed interface TransactionResult 
    permits Success, Failure {
    
    record Success(
        String transactionId,
        String authorizationCode,
        BigDecimal amount
    ) implements TransactionResult {}
    
    sealed interface Failure extends TransactionResult 
        permits InsufficientFunds, ExpiredCard, Declined {
        String reason();
    }
    
    record InsufficientFunds(String reason) implements Failure {}
    record ExpiredCard(String reason) implements Failure {}
    record Declined(String reason) implements Failure {}
}
```

**Usage with Pattern Matching:**
```java
String message = switch (result) {
    case Success s -> "Approved: " + s.authorizationCode();
    case InsufficientFunds f -> "Declined: " + f.reason();
    case ExpiredCard e -> "Declined: " + e.reason();
    case Declined d -> "Declined: " + d.reason();
};
```

### 3.3 Text Blocks (JEP 378) - LOW PRIORITY

**Benefits:** Improved readability for multi-line strings

**Candidate: JPQL Query in TransactionRepository**

```java
// Current
@Query("SELECT t FROM Transaction t ORDER BY t.createdAt DESC")
List<Transaction> findTop50ByOrderByCreatedAtDesc();

// With Text Block (if query becomes complex)
@Query("""
    SELECT t FROM Transaction t 
    WHERE t.status = :status 
    AND t.createdAt >= :startDate
    ORDER BY t.createdAt DESC
    """)
List<Transaction> findByStatusAndDateRange(
    @Param("status") TransactionStatus status,
    @Param("startDate") LocalDateTime startDate
);
```

### 3.4 Pattern Matching for instanceof (JEP 394) - LOW PRIORITY

**Current Usage:** None found in codebase  
**Opportunity:** Can be used in future exception handling

```java
// Future enhancement example
if (exception instanceof ValidationException ve) {
    return ResponseEntity.badRequest()
        .body(buildErrorResponse(ve.getMessage()));
} else if (exception instanceof DataAccessException dae) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(buildErrorResponse("Database error: " + dae.getMessage()));
}
```

### 3.5 Enhanced NullPointerException Messages (JEP 358)

**Benefits:** Automatic - better debugging with detailed NPE messages

**Example:**
```
// Java 11
NullPointerException
    at PaymentService.authorize(PaymentService.java:38)

// Java 17
NullPointerException: Cannot invoke "String.length()" because "request.getCardNumber()" is null
    at PaymentService.authorize(PaymentService.java:38)
```

---

## 4. Updated pom.xml Changes

### 4.1 Key Changes Summary

| Property/Dependency | Old Value | New Value | Reason |
|---------------------|-----------|-----------|---------|
| Spring Boot Parent | 2.7.18 | 3.2.5 | Java 17 support, jakarta.* namespace |
| java.version | 11 | 17 | Target Java version |
| maven.compiler.source | 11 | 17 | Compiler source version |
| maven.compiler.target | 11 | 17 | Compiler target version |
| maven-compiler-plugin | 3.8.1 | 3.11.0 | Better Java 17 support |

### 4.2 Complete Updated pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version> <!-- Updated from 2.7.18 -->
        <relativePath/>
    </parent>

    <groupId>com.demo</groupId>
    <artifactId>payment-app</artifactId>
    <version>1.0.0</version>
    <name>payment-app</name>
    <description>Mock Credit Card Payment Processing Application</description>

    <properties>
        <java.version>17</java.version> <!-- Updated from 11 -->
        <maven.compiler.source>17</maven.compiler.source> <!-- Updated from 11 -->
        <maven.compiler.target>17</maven.compiler.target> <!-- Updated from 11 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- All dependencies remain the same - versions managed by Spring Boot parent -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version> <!-- Updated from 3.8.1 -->
                <configuration>
                    <source>17</source> <!-- Updated from 11 -->
                    <target>17</target> <!-- Updated from 11 -->
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 4.3 Dependency Compatibility

All current dependencies are compatible with Spring Boot 3.2.x:
- ✅ H2 Database - Auto-managed by Spring Boot
- ✅ Caffeine Cache - Compatible
- ✅ Micrometer - Compatible
- ✅ Spring Data JPA - Compatible
- ✅ Spring Validation - Compatible (uses jakarta.validation)

---

## 5. Estimated Effort & Risk Assessment

### 5.1 Effort Estimation

| Task | Complexity | Estimated Hours | Risk Level |
|------|-----------|-----------------|------------|
| **Phase 1: Setup & Dependencies** | | | |
| Update pom.xml | Low | 0.5 | Low |
| Update Dockerfile | Low | 0.5 | Low |
| Verify build | Low | 1 | Low |
| **Phase 2: Namespace Migration** | | | |
| javax.* → jakarta.* changes | Medium | 2 | Medium |
| Fix compilation errors | Medium | 2 | Medium |
| Update imports | Low | 1 | Low |
| **Phase 3: Code Modernization** | | | |
| Convert PaymentRequest to Record | Medium | 3 | Medium |
| Convert PaymentResponse to Record | Medium | 3 | Medium |
| Update builder pattern usage | Medium | 2 | Medium |
| Modernize switch statements | Low | 1 | Low |
| Refactor Optional patterns | Low | 1 | Low |
| **Phase 4: Testing & Validation** | | | |
| Unit test updates | Medium | 4 | Medium |
| Integration testing | High | 6 | High |
| Performance testing | Medium | 3 | Medium |
| Security validation | Medium | 2 | Medium |
| **Phase 5: Documentation** | | | |
| Update README | Low | 1 | Low |
| API documentation | Low | 1 | Low |
| Deployment guide | Low | 1 | Low |
| **Total** | | **35 hours** | |

### 5.2 Risk Assessment Matrix

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|---------------------|
| **Breaking changes in Spring Boot 3.x** | High | High | Thorough testing, staged rollout |
| **javax → jakarta namespace issues** | High | High | Automated find/replace, comprehensive testing |
| **Record conversion breaks serialization** | Medium | Medium | Test JSON serialization thoroughly |
| **Performance regression** | Low | Medium | Benchmark before/after, load testing |
| **Third-party library incompatibility** | Low | High | Verify all dependencies before migration |
| **Docker image size increase** | Low | Low | Use distroless Java 17 image |
| **Production deployment issues** | Medium | High | Blue-green deployment, rollback plan |

---

## 6. Recommended Order of Changes (Minimize Risk)

### Phase 1: Foundation (Week 1) - CRITICAL PATH
**Goal:** Establish Java 17 environment without code changes

1. **Update pom.xml** ⚠️ CRITICAL
   - Change Java version to 17
   - Update Spring Boot to 3.2.5
   - Update maven-compiler-plugin to 3.11.0
   - **Risk:** Low | **Effort:** 0.5 hours

2. **Update Dockerfile** ⚠️ CRITICAL
   - Change builder: `maven:3.9-openjdk-17`
   - Change runtime: `gcr.io/distroless/java17-debian12`
   - **Risk:** Low | **Effort:** 0.5 hours

3. **Verify Build** ⚠️ CRITICAL
   - Run `mvn clean compile` (expect failures)
   - Document all compilation errors
   - **Risk:** Low | **Effort:** 1 hour

**Checkpoint:** Build environment ready for Java 17

---

### Phase 2: Critical Namespace Migration (Week 1-2) - BLOCKING
**Goal:** Fix all breaking changes from Spring Boot 3.x

4. **javax.* → jakarta.* Migration** ⚠️ BLOCKING
   - Update all imports in:
     - `PaymentRequest.java` (lines 3-5)
     - `PaymentController.java` (line 11)
     - `Transaction.java` (line 3)
   - **Risk:** Medium | **Effort:** 2 hours

5. **Fix Compilation Errors** ⚠️ BLOCKING
   - Address Spring Boot 3.x API changes
   - Update deprecated method calls
   - **Risk:** Medium | **Effort:** 2 hours

6. **Verify Application Starts** ⚠️ BLOCKING
   - Run application locally
   - Test basic endpoints
   - Check H2 console access
   - **Risk:** Medium | **Effort:** 1 hour

**Checkpoint:** Application compiles and runs on Java 17

---

### Phase 3: Code Modernization (Week 2-3) - ENHANCEMENT
**Goal:** Leverage Java 17 features for better code quality

7. **Modernize Switch Statements** ✨ QUICK WIN
   - Update `PaymentService.getDeclineMessage()`
   - Use switch expressions
   - **Risk:** Low | **Effort:** 1 hour

8. **Refactor Optional Patterns** ✨ QUICK WIN
   - Update `PaymentController.getTransaction()`
   - Use functional style with map/orElseGet
   - **Risk:** Low | **Effort:** 1 hour

9. **Convert PaymentResponse to Record** 📦 HIGH VALUE
   - Create record version
   - Add factory methods to replace builder
   - Update all usages
   - Test JSON serialization
   - **Risk:** Medium | **Effort:** 3 hours

10. **Convert PaymentRequest to Record** 📦 HIGH VALUE
    - Create record version
    - Ensure validation annotations work
    - Update all usages
    - Test JSON deserialization
    - **Risk:** Medium | **Effort:** 3 hours

11. **Update Builder Pattern Usage** 🔧 REQUIRED
    - Replace builder calls with record constructors
    - Update factory methods
    - **Risk:** Medium | **Effort:** 2 hours

**Checkpoint:** Modernized codebase with Java 17 features

---

### Phase 4: Testing & Validation (Week 3-4) - QUALITY GATE
**Goal:** Ensure reliability and performance

12. **Unit Testing** ✅ REQUIRED
    - Update existing tests for record changes
    - Add tests for new patterns
    - Achieve 80%+ code coverage
    - **Risk:** Medium | **Effort:** 4 hours

13. **Integration Testing** ✅ REQUIRED
    - Test all API endpoints
    - Verify database operations
    - Test caching behavior
    - Validate error handling
    - **Risk:** High | **Effort:** 6 hours

14. **Performance Testing** 📊 REQUIRED
    - Benchmark key operations
    - Compare with Java 11 baseline
    - Load testing with JMeter/Gatling
    - Memory profiling
    - **Risk:** Medium | **Effort:** 3 hours

15. **Security Validation** 🔒 REQUIRED
    - Run security scans
    - Verify dependency vulnerabilities
    - Test authentication/authorization
    - **Risk:** Medium | **Effort:** 2 hours

**Checkpoint:** Fully tested and validated application

---

### Phase 5: Documentation & Deployment (Week 4) - DELIVERY
**Goal:** Prepare for production deployment

16. **Update Documentation** 📝 REQUIRED
    - Update README with Java 17 requirements
    - Document new features used
    - Update API documentation
    - Create migration guide
    - **Risk:** Low | **Effort:** 3 hours

17. **Deployment Preparation** 🚀 REQUIRED
    - Build Docker image
    - Test in staging environment
    - Create deployment runbook
    - Prepare rollback plan
    - **Risk:** Medium | **Effort:** 2 hours

18. **Production Deployment** 🎯 CRITICAL
    - Deploy to production (canary/blue-green)
    - Monitor metrics and logs
    - Validate functionality
    - **Risk:** High | **Effort:** 4 hours

**Checkpoint:** Production-ready Java 17 application

---

## 7. Success Criteria

### 7.1 Technical Criteria

- ✅ Application compiles without errors on Java 17
- ✅ All unit tests pass (80%+ coverage)
- ✅ All integration tests pass
- ✅ No performance regression (< 5% acceptable)
- ✅ No security vulnerabilities introduced
- ✅ Docker image builds successfully
- ✅ Application runs in all environments

### 7.2 Business Criteria

- ✅ Zero downtime deployment
- ✅ No data loss or corruption
- ✅ All API endpoints functional
- ✅ Response times within SLA
- ✅ Error rates < 0.1%

### 7.3 Code Quality Criteria

- ✅ Records used for DTOs (PaymentRequest, PaymentResponse)
- ✅ Modern switch expressions implemented
- ✅ Optional patterns modernized
- ✅ Code coverage maintained or improved
- ✅ No deprecated API usage
- ✅ Documentation updated

---

## 8. Rollback Plan

### 8.1 Rollback Triggers

- Critical bugs in production
- Performance degradation > 20%
- Security vulnerabilities discovered
- Data integrity issues
- Unresolved compatibility issues

### 8.2 Rollback Procedure

1. **Immediate Actions**
   - Stop new deployments
   - Assess impact and severity
   - Notify stakeholders

2. **Rollback Steps**
   - Revert to previous Docker image tag
   - Restart application pods/containers
   - Verify application health
   - Monitor for stability

3. **Post-Rollback**
   - Document issues encountered
   - Analyze root cause
   - Plan remediation
   - Schedule retry

---

## 9. Timeline Summary

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| Phase 1: Foundation | 1 week | Java 17 environment ready |
| Phase 2: Namespace Migration | 1 week | Application compiles and runs |
| Phase 3: Code Modernization | 2 weeks | Modern Java 17 features implemented |
| Phase 4: Testing & Validation | 1 week | Fully tested application |
| Phase 5: Documentation & Deployment | 1 week | Production deployment |
| **Total** | **6 weeks** | Java 17 in production |

---

## Appendix A: File-by-File Change Summary

| File | Changes Required | Priority | Effort |
|------|------------------|----------|--------|
| pom.xml | Update versions | Critical | 0.5h |
| Dockerfile | Update base images | Critical | 0.5h |
| PaymentRequest.java | javax→jakarta, convert to record | High | 3h |
| PaymentResponse.java | Convert to record | High | 3h |
| PaymentController.java | javax→jakarta, Optional patterns | High | 2h |
| Transaction.java | javax→jakarta | High | 1h |
| PaymentService.java | Switch expressions, builder updates | Medium | 2h |
| TransactionStatus.java | Optional: sealed classes | Low | 1h |
| CacheService.java | No changes required | - | 0h |
| CacheConfig.java | No changes required | - | 0h |
| AdminController.java | No changes required | - | 0h |
| TransactionRepository.java | Optional: text blocks | Low | 0.5h |
| application.properties | Verify compatibility | Low | 0.5h |

---

## Appendix B: Reference Links

- [Java 17 Release Notes](https://www.oracle.com/java/technologies/javase/17-relnote-issues.html)
- [Spring Boot 3.x Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [JEP 395: Records](https://openjdk.org/jeps/395)
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409)
- [JEP 378: Text Blocks](https://openjdk.org/jeps/378)
- [JEP 394: Pattern Matching for instanceof](https://openjdk.org/jeps/394)
- [Jakarta EE 9 Namespace Migration](https://jakarta.ee/specifications/platform/9/jakarta-platform-spec-9.html)

---

**Document Status:** READY FOR REVIEW  
**Prepared By:** Java Modernization Team  
**Review Date:** June 4, 2026  
**Approval Required:** Technical Lead, Product Owner, DevOps Lead