# Java 11 to Java 17 Modernization Plan
## Payment Processing Application

**Document Version:** 1.0  
**Date:** 2026-05-28  
**Current Java Version:** 11  
**Target Java Version:** 17  
**Spring Boot Version:** 2.7.18 → 3.x (recommended)

---

## Executive Summary

This document outlines the comprehensive plan to modernize the Payment Processing Application from Java 11 to Java 17. The migration will leverage new language features, improve code quality, enhance performance, and ensure compatibility with modern frameworks.

**Key Benefits:**
- Enhanced performance (G1GC improvements, better JIT compilation)
- Improved code readability with Records, Sealed Classes, and Text Blocks
- Better pattern matching capabilities
- Stronger encapsulation and type safety
- Long-term support (Java 17 is an LTS release until September 2029)

---

## 1. Summary of Changes Required

### 1.1 Dependency Updates

| Component | Current Version | Target Version | Reason |
|-----------|----------------|----------------|---------|
| Java | 11 | 17 | LTS upgrade |
| Spring Boot | 2.7.18 | 3.2.x | Java 17 compatibility, Jakarta EE 9+ |
| Maven Compiler Plugin | 3.8.1 | 3.11.0 | Java 17 support |
| H2 Database | (inherited) | 2.2.x | Compatibility |
| Caffeine Cache | (inherited) | Latest | Performance improvements |

### 1.2 API Changes

**Deprecated/Removed APIs:**
- `javax.*` packages → `jakarta.*` (Spring Boot 3.x requirement)
- `Optional.isPresent()` → `Optional.isEmpty()` (available since Java 11, but better pattern matching in 17)

### 1.3 Configuration Changes

**pom.xml:**
- Update Java version properties (11 → 17)
- Update Spring Boot parent version (2.7.18 → 3.2.x)
- Update maven-compiler-plugin version and configuration
- Update Dockerfile base images (Java 11 → Java 17)

**application.properties:**
- Review and update deprecated Spring Boot 3.x properties
- Update CORS configuration (Spring Boot 3.x changes)

---

## 2. Deprecated/Removed APIs Requiring Replacement

### 2.1 Jakarta EE Migration (Spring Boot 3.x)

**Impact:** HIGH  
**Files Affected:** All files using `javax.*` imports

| Old Package | New Package | Files Affected |
|-------------|-------------|----------------|
| `javax.persistence.*` | `jakarta.persistence.*` | Transaction.java |
| `javax.validation.*` | `jakarta.validation.*` | PaymentRequest.java, PaymentController.java |

**Changes Required:**

```java
// BEFORE (Java 11 + Spring Boot 2.x)
import javax.persistence.*;
import javax.validation.constraints.*;

// AFTER (Java 17 + Spring Boot 3.x)
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
```

### 2.2 Optional API Improvements

**Impact:** LOW  
**Files Affected:** PaymentService.java, PaymentController.java

```java
// BEFORE
if (!optionalTransaction.isPresent()) {
    return buildErrorResponse(...);
}

// AFTER (More readable)
if (optionalTransaction.isEmpty()) {
    return buildErrorResponse(...);
}
```

---

## 3. New Java 17 Features for Code Improvement

### 3.1 Records (JEP 395) - HIGH PRIORITY

**Benefits:** Immutable data carriers, automatic equals/hashCode/toString, reduced boilerplate

**Recommended Conversions:**

#### 3.1.1 PaymentRequest → Record
**Current:** 138 lines with Builder pattern  
**After:** ~20 lines as Record

```java
// AFTER - PaymentRequest.java
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

**Effort:** 2 hours  
**Risk:** LOW (Records are immutable, ensure no setters are used)

#### 3.1.2 PaymentResponse → Record
**Current:** 140 lines with Builder pattern  
**After:** ~15 lines as Record

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
    // Builder pattern can be replaced with static factory methods
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

**Effort:** 2 hours  
**Risk:** LOW

**Note:** Transaction.java should remain a JPA Entity (not converted to Record) as JPA requires mutable entities.

### 3.2 Sealed Classes (JEP 409) - MEDIUM PRIORITY

**Benefits:** Restricted class hierarchies, exhaustive pattern matching

**Recommended:** TransactionStatus enum could be enhanced with sealed interface pattern for more complex status hierarchies

```java
// AFTER - Enhanced status hierarchy
public sealed interface TransactionStatus 
    permits SuccessStatus, FailureStatus {
    
    String getMessage();
}

public record SuccessStatus(String type, String authCode) 
    implements TransactionStatus {
    public String getMessage() {
        return "Transaction " + type + " successfully";
    }
}

public sealed interface FailureStatus extends TransactionStatus 
    permits DeclinedStatus, InsufficientFundsStatus, ExpiredCardStatus {
}

public record DeclinedStatus(String reason) implements FailureStatus {
    public String getMessage() {
        return "Transaction declined: " + reason;
    }
}
```

**Effort:** 4 hours  
**Risk:** MEDIUM (Requires refactoring service logic)  
**Recommendation:** Consider for Phase 2 if time permits

### 3.3 Text Blocks (JEP 378) - LOW PRIORITY

**Benefits:** Multi-line strings, improved readability for SQL/JSON/HTML

**Potential Use Cases:**
- SQL queries in TransactionRepository (if complex queries are added)
- Error messages
- Documentation strings

```java
// AFTER - Example for future SQL queries
@Query("""
    SELECT t FROM Transaction t 
    WHERE t.status = :status 
      AND t.createdAt BETWEEN :startDate AND :endDate
    ORDER BY t.createdAt DESC
    """)
List<Transaction> findByStatusAndDateRange(
    @Param("status") TransactionStatus status,
    @Param("startDate") LocalDateTime startDate,
    @Param("endDate") LocalDateTime endDate
);
```

**Effort:** 1 hour  
**Risk:** LOW  
**Recommendation:** Apply opportunistically during other changes

### 3.4 Pattern Matching for instanceof (JEP 394) - LOW PRIORITY

**Benefits:** Reduced casting boilerplate

**Current Usage:** Not heavily used in current codebase  
**Recommendation:** Apply in exception handling if expanded

```java
// AFTER - Example pattern
if (exception instanceof ValidationException ve) {
    return buildErrorResponse(ve.getMessage());
} else if (exception instanceof DataAccessException dae) {
    return buildErrorResponse("Database error: " + dae.getMessage());
}
```

**Effort:** 1 hour  
**Risk:** LOW

### 3.5 Switch Expressions (JEP 361) - MEDIUM PRIORITY

**Benefits:** More concise, expression-based switches

**Recommended:** PaymentService.getDeclineMessage()

```java
// BEFORE
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

// AFTER
private String getDeclineMessage(TransactionStatus status) {
    return switch (status) {
        case INSUFFICIENT_FUNDS -> "Transaction declined: Insufficient funds";
        case EXPIRED_CARD -> "Transaction declined: Card expired";
        case DECLINED -> "Transaction declined";
        default -> "Transaction declined";
    };
}
```

**Effort:** 1 hour  
**Risk:** LOW

---

## 4. Updated pom.xml Changes

### 4.1 Key Changes Summary

| Property/Dependency | Current | Target | Notes |
|---------------------|---------|--------|-------|
| java.version | 11 | 17 | Core Java version |
| maven.compiler.source | 11 | 17 | Compiler source level |
| maven.compiler.target | 11 | 17 | Compiler target level |
| spring-boot-starter-parent | 2.7.18 | 3.2.5 | Major version upgrade |
| maven-compiler-plugin | 3.8.1 | 3.11.0 | Java 17 support |

### 4.2 Dockerfile Updates

**Build Stage:**
- FROM maven:3.8-openjdk-11 → maven:3.9-eclipse-temurin-17

**Runtime Stage:**
- FROM gcr.io/distroless/java11-debian11 → gcr.io/distroless/java17-debian12

---

## 5. Estimated Effort and Risk Assessment

### 5.1 Change Breakdown

| Task | Effort | Risk | Priority | Dependencies |
|------|--------|------|----------|--------------|
| **Phase 1: Foundation** |
| Update pom.xml (Java 17, Spring Boot 3.x) | 1 hour | LOW | P0 | None |
| Update Dockerfile | 0.5 hours | LOW | P0 | None |
| Replace javax.* → jakarta.* imports | 2 hours | MEDIUM | P0 | pom.xml update |
| Update application.properties | 1 hour | LOW | P0 | Spring Boot 3.x |
| Build and test compilation | 2 hours | MEDIUM | P0 | All above |
| **Phase 2: Code Modernization** |
| Convert PaymentRequest to Record | 2 hours | LOW | P1 | Phase 1 complete |
| Convert PaymentResponse to Record | 2 hours | LOW | P1 | Phase 1 complete |
| Update service layer for Records | 3 hours | MEDIUM | P1 | Records conversion |
| Apply Switch Expressions | 1 hour | LOW | P2 | Phase 1 complete |
| Apply Optional.isEmpty() | 1 hour | LOW | P2 | Phase 1 complete |
| **Phase 3: Testing & Validation** |
| Unit test updates | 4 hours | MEDIUM | P1 | Phase 2 complete |
| Integration testing | 4 hours | MEDIUM | P1 | Phase 2 complete |
| Performance testing | 2 hours | LOW | P2 | All above |
| Documentation updates | 2 hours | LOW | P2 | All above |
| **Optional: Advanced Features** |
| Sealed classes for status hierarchy | 4 hours | MEDIUM | P3 | Phase 2 complete |
| Text blocks for queries | 1 hour | LOW | P3 | Anytime |
| Pattern matching enhancements | 2 hours | LOW | P3 | Anytime |

### 5.2 Total Effort Estimate

| Phase | Estimated Time | Risk Level |
|-------|---------------|------------|
| Phase 1: Foundation | 6.5 hours | MEDIUM |
| Phase 2: Code Modernization | 9 hours | MEDIUM |
| Phase 3: Testing & Validation | 12 hours | MEDIUM |
| **Total (Required)** | **27.5 hours** | **MEDIUM** |
| Optional Advanced Features | 7 hours | LOW-MEDIUM |
| **Grand Total** | **34.5 hours** | **MEDIUM** |

**Recommended Timeline:** 1-2 weeks with proper testing

---

## 6. Recommended Order of Changes to Minimize Risk

### Phase 1: Foundation (Day 1-2)
**Goal:** Establish Java 17 + Spring Boot 3.x baseline

1. **Create feature branch** (`feature/java-17-migration`)
2. **Update pom.xml**
   - Java version: 11 → 17
   - Spring Boot: 2.7.18 → 3.2.5
   - Maven compiler plugin: 3.8.1 → 3.11.0
3. **Update Dockerfile**
   - Base images: Java 11 → Java 17
4. **Replace javax.* → jakarta.***
   - Transaction.java (JPA annotations)
   - PaymentRequest.java (validation annotations)
   - PaymentController.java (validation annotations)
5. **Update application.properties**
   - Review Spring Boot 3.x property changes
6. **Build and verify compilation**
   ```bash
   mvn clean compile
   ```
7. **Run existing tests**
   ```bash
   mvn test
   ```

**Checkpoint:** Application compiles and existing tests pass

### Phase 2: Code Modernization (Day 3-4)
**Goal:** Leverage Java 17 features for better code quality

1. **Convert PaymentRequest to Record**
   - Remove Builder class
   - Add static factory methods if needed
   - Update all usages in controllers/services
2. **Convert PaymentResponse to Record**
   - Remove Builder class
   - Add static factory methods (success/error)
   - Update all usages
3. **Apply Switch Expressions**
   - PaymentService.getDeclineMessage()
4. **Apply Optional.isEmpty()**
   - PaymentService.capture()
   - PaymentService.refund()
   - PaymentController.getTransaction()
5. **Code review and refactoring**

**Checkpoint:** All code compiles, manual testing passes

### Phase 3: Testing & Validation (Day 5-7)
**Goal:** Ensure reliability and performance

1. **Update unit tests**
   - Test Record constructors
   - Test factory methods
   - Verify validation still works
2. **Integration testing**
   - Test all REST endpoints
   - Verify database operations
   - Test cache functionality
3. **Performance testing**
   - Compare startup time (Java 11 vs 17)
   - Load testing with JMeter/Gatling
   - Memory profiling
4. **Documentation updates**
   - Update README
   - Update API documentation
   - Update deployment guides

**Checkpoint:** All tests pass, performance metrics acceptable

### Phase 4 (Optional): Advanced Features (Day 8-10)
**Goal:** Explore advanced Java 17 capabilities

1. **Sealed classes for TransactionStatus**
   - Design hierarchy
   - Implement sealed interfaces
   - Update service logic
2. **Text blocks for complex strings**
   - Apply to SQL queries
   - Apply to error messages
3. **Pattern matching enhancements**
   - Exception handling
   - Type checks

---

## 7. Risk Assessment

### 7.1 High-Risk Areas

| Risk | Impact | Mitigation Strategy |
|------|--------|---------------------|
| **Spring Boot 3.x Breaking Changes** | HIGH | Thorough testing of all endpoints, review Spring Boot 3.x migration guide |
| **Jakarta EE Migration** | HIGH | Automated find/replace with verification, comprehensive testing |
| **JPA Entity Compatibility** | MEDIUM | Keep Transaction as mutable entity, test persistence layer thoroughly |
| **Cache Configuration Changes** | MEDIUM | Verify Caffeine cache compatibility with Spring Boot 3.x |
| **Dependency Conflicts** | MEDIUM | Use `mvn dependency:tree` to identify conflicts |

### 7.2 Low-Risk Areas

| Area | Reason |
|------|--------|
| Records conversion | Immutable data carriers, well-tested feature |
| Switch expressions | Syntactic sugar, no runtime changes |
| Optional improvements | Minor API enhancements |
| Text blocks | String literals, no behavioral changes |

---

## 8. Testing Strategy

### 8.1 Test Categories

| Category | Focus | Tools |
|----------|-------|-------|
| **Unit Tests** | Individual components, Records, Services | JUnit 5, Mockito |
| **Integration Tests** | REST endpoints, Database, Cache | Spring Boot Test, TestContainers |
| **Performance Tests** | Throughput, Latency, Memory | JMeter, VisualVM |
| **Compatibility Tests** | Docker, Kubernetes deployment | Docker, kubectl |

### 8.2 Test Checklist

- [ ] All REST endpoints return correct responses
- [ ] Validation annotations work with Records
- [ ] JPA persistence operations succeed
- [ ] Cache operations function correctly
- [ ] Actuator endpoints accessible
- [ ] Prometheus metrics exported
- [ ] Docker image builds successfully
- [ ] Application starts in < 30 seconds
- [ ] Memory usage within acceptable limits
- [ ] No regression in transaction processing time

---

## 9. Success Criteria

### 9.1 Functional Requirements

- All existing features work identically
- No breaking changes to REST API
- Database schema unchanged
- Cache behavior consistent

### 9.2 Non-Functional Requirements

- Startup time: < 30 seconds (target: 20-25s)
- Memory usage: < 512MB heap (target: 400MB)
- Transaction processing: < 500ms p95 (target: 300ms)
- Code reduction: ~30% fewer lines (Records)
- Build time: < 2 minutes

### 9.3 Quality Metrics

- Test coverage: > 80%
- Zero critical vulnerabilities
- SonarQube quality gate: PASSED
- All compiler warnings resolved

---

## Appendix A: File-by-File Change Summary

| File | Changes Required | Effort | Risk |
|------|------------------|--------|------|
| **pom.xml** | Java 17, Spring Boot 3.2.5, Maven plugin | 1h | LOW |
| **Dockerfile** | Java 17 base images | 0.5h | LOW |
| **PaymentRequest.java** | Convert to Record, jakarta.validation | 2h | LOW |
| **PaymentResponse.java** | Convert to Record | 2h | LOW |
| **Transaction.java** | jakarta.persistence imports only | 0.5h | LOW |
| **TransactionStatus.java** | Optional: Sealed classes | 4h | MEDIUM |
| **PaymentService.java** | Switch expressions, Optional.isEmpty() | 2h | LOW |
| **PaymentController.java** | jakarta.validation imports | 0.5h | LOW |
| **AdminController.java** | No changes required | 0h | NONE |
| **CacheService.java** | No changes required | 0h | NONE |
| **CacheConfig.java** | Verify Spring Boot 3.x compatibility | 0.5h | LOW |
| **TransactionRepository.java** | No changes required | 0h | NONE |
| **application.properties** | Spring Boot 3.x property updates | 1h | LOW |

---

## Appendix B: Command Reference

### Build Commands
```bash
# Clean build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Run tests only
mvn test

# Check dependencies
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates
```

### Docker Commands
```bash
# Build image
docker build -t payment-app:1.0.0-java17 .

# Run container
docker run -p 8080:8080 payment-app:1.0.0-java17

# Check logs
docker logs <container-id>
```

### Testing Commands
```bash
# Run specific test
mvn test -Dtest=PaymentServiceTest

# Integration tests
mvn verify

# Performance test
mvn gatling:test
```

---

**Document Status:** READY FOR REVIEW  
**Next Steps:** Review with team, obtain approval, begin Phase 1  
**Approval Required:** Tech Lead, DevOps Lead

---

*End of Java 17 Modernization Plan*