# Payment Application API Test Commands

This document contains sample curl commands to test the Payment Application endpoints.

## 1. Health Check (Actuator)
Test if the application is running and healthy.

```bash
curl -X GET http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

---

## 2. Get Transaction History
Retrieve all recent transactions (initially empty).

```bash
curl -X GET http://localhost:8080/api/payments/history
```

**Expected Response:**
```json
[]
```

---

## 3. Authorize Payment
Create a new payment authorization.

```bash
curl -X POST http://localhost:8080/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4532015112830366",
    "amount": 99.99,
    "currency": "USD"
  }'
```

**Expected Response:**
```json
{
  "transactionId": "<generated-uuid>",
  "status": "AUTHORIZED",
  "authorizationCode": "<generated-auth-code>",
  "message": "Payment authorized successfully",
  "amount": 99.99,
  "currency": "USD"
}
```

---

## 4. Clear Cache (Admin)
Clear all application caches.

```bash
curl -X POST http://localhost:8080/admin/cache/clear
```

**Expected Response:**
```json
{
  "status": "success",
  "message": "All caches cleared successfully"
}
```

---

## Testing Sequence

Run the commands in this order to verify the application is working correctly:

1. **Health Check** - Verify the application is running
2. **Get Transaction History** - Verify the API is accessible (should return empty array)
3. **Authorize Payment** - Create a test transaction
4. **Clear Cache** - Verify admin endpoints are working

---

## Notes

- All endpoints support CORS with `origins = "*"`
- The application uses an in-memory H2 database
- Transaction data will be lost when the application restarts
- H2 Console is available at: http://localhost:8080/h2-console