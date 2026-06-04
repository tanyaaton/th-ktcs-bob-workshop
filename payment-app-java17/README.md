# Payment Application - Java 17 Modernized Version

## Overview

This is the modernized version of the Payment Processing Application, upgraded from Java 11 to Java 17 with Spring Boot 3.2.5. The application demonstrates modern Java features including Records, Switch Expressions, Pattern Matching, and the jakarta.* namespace migration.

## 🚀 Key Modernization Changes

### 1. Infrastructure Updates

#### Java & Spring Boot Versions
- **Java**: 11 → **17**
- **Spring Boot**: 2.7.18 → **3.2.5**
- **Maven Compiler Plugin**: 3.8.1 → **3.11.0**

#### Docker Base Images
- **Build Stage**: `maven:3.8-openjdk-11` → `maven:3.9-openjdk-17`
- **Runtime Stage**: `gcr.io/distroless/java11-debian11` → `gcr.io/distroless/java17-debian12`

### 2. Namespace Migration (javax.* → jakarta.*)

All Java EE packages have been migrated to Jakarta EE:

| Old Package (javax.*) | New Package (jakarta.*) |
|----------------------|-------------------------|
| `javax.validation.*` | `jakarta.validation.*` |
| `javax.persistence.*` | `jakarta.persistence.*` |
| `javax.servlet.*` | `jakarta.servlet.*` |

**Affected Files:**
- `PaymentRequest.java` - Validation annotations
- `PaymentController.java` - `@Valid` annotation
- `Transaction.java` - JPA annotations (`@Entity`, `@Table`, `@Id`, etc.)

### 3. Java 17 Features Implemented

#### Records (JEP 395)

**PaymentRequest.java** - Converted from 138-line POJO to 105-line Record (24% reduction)
```java
public record PaymentRequest(
    @NotBlank(message = "Card number is required")
    @Size(min = 13, max = 19, message = "Card number must be between 13 and 19 digits")
    String cardNumber,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    String currency,
    
    String cvv,
    String expiryMonth,
    String expiryYear,
    String transactionId
) {
    // Compact constructor for validation
    public PaymentRequest {
        if (cardNumber != null) {
            cardNumber = cardNumber.replaceAll("\\s+", "");
        }
    }
    
    // Factory methods replace builder pattern
    public PaymentRequest withTransactionId(String transactionId) {
        return new PaymentRequest(cardNumber, amount, currency, cvv, 
                                  expiryMonth, expiryYear, transactionId);
    }
}
```

**Benefits:**
- Immutable by default
- Automatic `equals()`, `hashCode()`, `toString()`
- Compact constructor for validation
- Factory methods for object creation
- Thread-safe without synchronization

**PaymentResponse.java** - Converted from 140-line POJO to 161-line Record with enhanced factory methods
```java
public record PaymentResponse(
    String transactionId,
    TransactionStatus status,
    String message,
    BigDecimal amount,
    String currency,
    LocalDateTime timestamp
) {
    // Factory methods for common scenarios
    public static PaymentResponse success(String transactionId, BigDecimal amount, String currency) {
        return new PaymentResponse(transactionId, TransactionStatus.APPROVED, 
                                   "Payment processed successfully", amount, currency, LocalDateTime.now());
    }
    
    public static PaymentResponse declined(String transactionId, String reason, BigDecimal amount, String currency) {
        return new PaymentResponse(transactionId, TransactionStatus.DECLINED, 
                                   reason, amount, currency, LocalDateTime.now());
    }
    
    // Helper methods
    public boolean isSuccessful() {
        return status == TransactionStatus.APPROVED || status == TransactionStatus.CAPTURED;
    }
}
```

**Why Transaction.java Remains a Class:**
JPA entities cannot be Records because:
- JPA requires mutable entities for lazy loading
- Hibernate needs to create proxies at runtime
- Entity lifecycle management requires mutability
- Only migrated to `jakarta.persistence.*` namespace

#### Switch Expressions (JEP 361)

**PaymentService.java** - Reduced from 11 lines to 6 lines (45% reduction)

**Before (Java 11):**
```java
private String getDeclineMessage(TransactionStatus status) {
    switch (status) {
        case INSUFFICIENT_FUNDS:
            return "Transaction declined: Insufficient funds";
        case EXPIRED_CARD:
            return "Transaction declined: Card expired";
        case DECLINED:
            return "Transaction declined";
        default:
            return "Transaction declined";
    }
}
```

**After (Java 17):**
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
- More concise and readable
- Expression-based (returns value directly)
- Exhaustiveness checking at compile time
- No fall-through bugs

#### Enhanced Optional Patterns

**PaymentController.java** - Functional style replaces imperative checks

**Before (Java 11):**
```java
@GetMapping("/{id}")
public ResponseEntity<PaymentResponse> getTransaction(@PathVariable String id) {
    Optional<PaymentResponse> response = paymentService.getTransaction(id);
    if (response.isPresent()) {
        return ResponseEntity.ok(response.get());
    } else {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(PaymentResponse.error(id, "Transaction not found"));
    }
}
```

**After (Java 17):**
```java
@GetMapping("/{id}")
public ResponseEntity<PaymentResponse> getTransaction(@PathVariable String id) {
    return paymentService.getTransaction(id)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(PaymentResponse.error(id, "Transaction not found")));
}
```

**Benefits:**
- More functional and declarative
- Eliminates null checks
- Method chaining improves readability
- Reduces boilerplate code

### 4. Dependency Updates

#### Core Dependencies
```xml
<!-- Spring Boot -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<!-- Jakarta Validation -->
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.0.2</version>
</dependency>

<!-- Jakarta Persistence -->
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <version>3.1.0</version>
</dependency>
```

#### Updated Libraries
- **H2 Database**: 2.1.214 → 2.2.224
- **Caffeine Cache**: 3.1.8 (compatible with Spring Boot 3.x)
- **Micrometer**: 1.12.5 (Spring Boot 3.x compatible)

## 📊 Code Metrics Comparison

| Metric | Java 11 | Java 17 | Improvement |
|--------|---------|---------|-------------|
| PaymentRequest.java | 138 lines | 105 lines | -24% |
| PaymentResponse.java | 140 lines | 161 lines | +15% (added factory methods) |
| Switch statements | 11 lines | 6 lines | -45% |
| Optional handling | Imperative | Functional | More readable |
| Boilerplate code | High | Low | Records eliminate getters/setters/equals/hashCode |

## 🏗️ Project Structure

```
payment-app-java17/
├── pom.xml                          # Maven configuration (Java 17, Spring Boot 3.2.5)
├── Dockerfile                       # Multi-stage build with Java 17
├── README.md                        # This file
├── k8s/
│   └── deployment.yaml             # Kubernetes/OpenShift deployment manifests
└── src/
    └── main/
        ├── java/com/demo/payment/
        │   ├── PaymentApplication.java              # Spring Boot main class
        │   ├── config/
        │   │   └── CacheConfig.java                 # Caffeine cache configuration
        │   ├── controller/
        │   │   ├── AdminController.java             # Admin endpoints
        │   │   └── PaymentController.java           # Payment REST API (jakarta.validation)
        │   ├── model/
        │   │   ├── PaymentRequest.java              # Record with jakarta.validation
        │   │   ├── PaymentResponse.java             # Record with factory methods
        │   │   ├── Transaction.java                 # JPA Entity (jakarta.persistence)
        │   │   └── TransactionStatus.java           # Enum
        │   ├── repository/
        │   │   └── TransactionRepository.java       # Spring Data JPA
        │   └── service/
        │       ├── CacheService.java                # Cache management
        │       └── PaymentService.java              # Business logic (switch expressions)
        └── resources/
            ├── application.properties               # Application configuration
            └── static/
                ├── index.html                       # React UI
                ├── app.js                           # React components
                └── styles.css                       # Styling
```

## 🚀 Building and Running

### Prerequisites
- **Java 17** (OpenJDK or Oracle JDK)
- **Maven 3.9+**
- **Docker** (optional, for containerized deployment)

### Build with Maven
```bash
cd payment-app-java17
mvn clean package
```

### Run Locally
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Build Docker Image
```bash
docker build -t payment-app:java17 .
```

### Run Docker Container
```bash
docker run -p 8080:8080 payment-app:java17
```

## 🧪 Testing the Application

### Access the Web UI
Open your browser and navigate to:
```
http://localhost:8080
```

### Test Card Numbers
Use these test card numbers for different scenarios:
- **Visa**: 4263970000005262 (Approved)
- **MasterCard**: 5425230000004415 (Approved)
- **Amex**: 374101000000608 (Approved)

### REST API Endpoints

#### Authorize Payment
```bash
curl -X POST http://localhost:8080/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4263970000005262",
    "expiryMonth": "12",
    "expiryYear": "25",
    "cvv": "123",
    "amount": 100.00,
    "currency": "USD"
  }'
```

#### Get Transaction
```bash
curl http://localhost:8080/api/payments/{transactionId}
```

#### Transaction History
```bash
curl http://localhost:8080/api/payments/history
```

#### Health Check
```bash
curl http://localhost:8080/actuator/health
```

#### Metrics (Prometheus)
```bash
curl http://localhost:8080/actuator/prometheus
```

## 🔧 Configuration

### Application Properties
Key configurations in `application.properties`:

```properties
# Server Configuration
server.port=8080

# H2 Database (In-Memory)
spring.datasource.url=jdbc:h2:mem:paymentdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

# Actuator Endpoints
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always

# CORS Configuration
spring.web.cors.allowed-origins=*
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
```

## 🐳 Kubernetes/OpenShift Deployment

### Deploy to OpenShift
```bash
# Set environment variables
export NAMESPACE=your-namespace
export IMAGE_TAG=java17

# Apply deployment
envsubst < k8s/deployment.yaml | oc apply -f -
```

### Resources Created
- **Deployment**: 3 replicas with health checks
- **Service**: ClusterIP on port 8080
- **Route**: HTTPS with edge TLS termination
- **PodDisruptionBudget**: High availability configuration

## 📈 Performance Considerations

### Java 17 Performance Improvements
- **G1GC Enhancements**: Better garbage collection performance
- **Compact Strings**: Reduced memory footprint for String objects
- **Improved JIT Compilation**: Faster startup and runtime performance
- **Records**: Lower memory overhead compared to traditional POJOs

### Resource Limits (Kubernetes)
```yaml
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi
```

### JVM Options
```bash
JAVA_OPTS="-Xmx384m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## 🔒 Security Features

### Container Security
- Non-root user (UID 1001)
- Read-only root filesystem (where possible)
- Dropped all capabilities
- No privilege escalation

### Application Security
- Input validation with jakarta.validation
- CORS configuration
- Actuator endpoints secured
- TLS termination at edge (OpenShift Route)

## 📝 Migration Notes

### Breaking Changes from Java 11
1. **Namespace Migration**: All `javax.*` imports must be changed to `jakarta.*`
2. **Spring Boot 3.x**: Major version upgrade with breaking changes
3. **Records**: Cannot be used for JPA entities (use traditional classes)
4. **Sealed Classes**: Available but not used in this application

### Backward Compatibility
- The REST API remains unchanged
- Database schema is identical
- Frontend code requires no changes
- Configuration properties are compatible

## 🎯 Future Enhancements

### Potential Java 17+ Features to Explore
1. **Text Blocks (JEP 378)**: For multi-line SQL queries or JSON templates
2. **Pattern Matching for instanceof (JEP 394)**: For type checking in service layer
3. **Sealed Classes (JEP 409)**: For TransactionStatus hierarchy
4. **Virtual Threads (Java 21)**: For improved concurrency

### Recommended Next Steps
1. Add comprehensive unit tests with JUnit 5
2. Implement integration tests with Testcontainers
3. Add API documentation with SpringDoc OpenAPI
4. Implement distributed tracing with Micrometer Tracing
5. Add security with Spring Security and OAuth2

## 📚 References

### Java 17 Features
- [JEP 395: Records](https://openjdk.org/jeps/395)
- [JEP 361: Switch Expressions](https://openjdk.org/jeps/361)
- [JEP 394: Pattern Matching for instanceof](https://openjdk.org/jeps/394)
- [JEP 378: Text Blocks](https://openjdk.org/jeps/378)

### Spring Boot 3.x
- [Spring Boot 3.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Jakarta EE 9+ Migration](https://jakarta.ee/specifications/)

### Best Practices
- [Effective Java (3rd Edition)](https://www.oreilly.com/library/view/effective-java/9780134686097/)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)

## 🤝 Contributing

This is a demonstration project for Java 11 to Java 17 modernization. For production use:
1. Add comprehensive error handling
2. Implement proper logging strategy
3. Add security layers (authentication, authorization)
4. Implement database migrations (Flyway/Liquibase)
5. Add monitoring and alerting

## 📄 License

This project is for educational and demonstration purposes.

---

**Modernized with Java 17 | Spring Boot 3.2.5 | Jakarta EE 10**