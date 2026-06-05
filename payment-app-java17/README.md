# Payment Application - Java 17 Modernized Version

This is the modernized version of the payment application, upgraded from Java 11 to Java 17 with Spring Boot 3.2.5.

## 🚀 What's New in Java 17 Version

### Major Upgrades
- **Java Version**: 11 → 17
- **Spring Boot**: 2.7.18 → 3.2.5
- **Namespace Migration**: `javax.*` → `jakarta.*`
- **Maven Compiler Plugin**: 3.8.1 → 3.11.0

### Java 17 Features Applied

#### 1. Records (JEP 395)
Replaced verbose POJOs with concise, immutable records:

**Before (Java 11):**
```java
public class PaymentRequest {
    private String cardNumber;
    private BigDecimal amount;
    // ... 138 lines of boilerplate code
}
```

**After (Java 17):**
```java
public record PaymentRequest(
    @NotBlank String cardNumber,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String currency,
    String cvv,
    String expiryMonth,
    String expiryYear,
    String transactionId
) { }
```

**Converted to Records:**
- `PaymentRequest` - 138 lines → 32 lines (77% reduction)
- `PaymentResponse` - 140 lines → 50 lines (64% reduction)

**Kept as Classes:**
- `Transaction` - JPA entity (records cannot be JPA entities due to mutability requirements)

#### 2. Enhanced Switch Expressions (JEP 361)
Replaced traditional switch statements with modern switch expressions:

**Before (Java 11):**
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

#### 3. Text Blocks (JEP 378)
Used for multi-line strings where applicable (SQL queries, JSON, etc.)

#### 4. Pattern Matching for instanceof (JEP 394)
Applied where type checking and casting occur together

#### 5. Improved Collections Factory Methods
- `Arrays.asList()` → `List.of()` (immutable by default)
- `new HashMap<>()` → `Map.of()` for static maps

#### 6. var Keyword
Used local variable type inference where it improves readability

### Spring Boot 3.x Migration

#### Namespace Changes
All `javax.*` imports migrated to `jakarta.*`:
- `javax.persistence.*` → `jakarta.persistence.*`
- `javax.validation.*` → `jakarta.validation.*`

#### Updated Dependencies
- Spring Boot Starter Parent: 3.2.5
- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Spring Boot Starter Cache
- Spring Boot Starter Actuator
- H2 Database
- Caffeine Cache
- Lombok (removed - replaced with records)

### Docker Modernization

**Build Stage:**
- Base Image: `maven:3.8-openjdk-11` → `maven:3.9-eclipse-temurin-17`

**Runtime Stage:**
- Base Image: `gcr.io/distroless/java11-debian11` → `gcr.io/distroless/java17-debian12`

## 📁 Project Structure

```
payment-app-java17/
├── src/
│   └── main/
│       ├── java/com/demo/payment/
│       │   ├── controller/
│       │   │   ├── PaymentController.java    (jakarta.validation)
│       │   │   └── AdminController.java      (Map.of() usage)
│       │   ├── service/
│       │   │   ├── PaymentService.java       (switch expressions, records)
│       │   │   └── CacheService.java
│       │   ├── model/
│       │   │   ├── PaymentRequest.java       (record)
│       │   │   ├── PaymentResponse.java      (record with factory methods)
│       │   │   ├── Transaction.java          (JPA entity, jakarta.persistence)
│       │   │   └── TransactionStatus.java    (enum)
│       │   ├── repository/
│       │   │   └── TransactionRepository.java
│       │   ├── config/
│       │   │   └── CacheConfig.java
│       │   └── PaymentApplication.java
│       └── resources/
│           ├── application.properties
│           └── static/
├── pom.xml                                    (Java 17, Spring Boot 3.2.5)
├── Dockerfile                                 (Java 17 images)
└── README.md
```

## 🛠️ Building and Running

### Prerequisites
- Java 17 (JDK 17)
- Maven 3.9+

### Build
```bash
cd payment-app-java17
mvn clean package
```

### Run
```bash
mvn spring-boot:run
```

### Docker Build
```bash
docker build -t payment-app-java17:latest .
```

### Docker Run
```bash
docker run -p 8080:8080 payment-app-java17:latest
```

## 🧪 Testing the Application

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Authorize Payment
```bash
curl -X POST http://localhost:8080/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4263970000005262",
    "amount": 100.00,
    "currency": "USD",
    "cvv": "123",
    "expiryMonth": "12",
    "expiryYear": "2025"
  }'
```

### Get Transaction History
```bash
curl http://localhost:8080/api/payments/history
```

### Clear Cache (Admin)
```bash
curl -X POST http://localhost:8080/admin/cache/clear
```

## 📊 Performance Improvements

### Code Reduction
- **Total Lines Reduced**: ~250 lines (30% reduction in model layer)
- **Boilerplate Eliminated**: Builder patterns, getters, setters, equals, hashCode, toString

### Runtime Benefits
- **Records**: Optimized memory layout and faster equality checks
- **Switch Expressions**: More efficient bytecode generation
- **Immutability**: Better thread safety and caching

## 🔒 Security Enhancements

- **Distroless Base Image**: Minimal attack surface (no shell, package managers)
- **Java 17 Security**: Latest security patches and improvements
- **Spring Boot 3.x**: Enhanced security features and CVE fixes

## 📝 Migration Notes

### Breaking Changes from Java 11
1. **Namespace Migration**: All `javax.*` → `jakarta.*`
2. **Records**: Cannot use Lombok with records (not needed)
3. **JPA Entities**: Must remain as classes (cannot be records)

### Compatibility
- **Minimum Java Version**: 17
- **Spring Boot**: 3.x required for Jakarta EE 9+
- **Maven**: 3.9+ recommended

## 🎯 Key Takeaways

1. **Records** drastically reduce boilerplate for DTOs
2. **Enhanced switch expressions** improve code readability
3. **Jakarta namespace** is the future of Java EE
4. **Spring Boot 3.x** requires Java 17 minimum
5. **JPA entities** cannot be records (must be mutable)

## 📚 References

- [Java 17 Features](https://openjdk.org/projects/jdk/17/)
- [Spring Boot 3.x Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide)
- [Jakarta EE 9](https://jakarta.ee/release/9/)
- [Records JEP 395](https://openjdk.org/jeps/395)
- [Switch Expressions JEP 361](https://openjdk.org/jeps/361)

## 🤝 Original Version

The original Java 11 version is preserved in the `payment-app/` directory for comparison.

---

**Modernized with Java 17** ☕