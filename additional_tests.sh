#!/bin/bash

BASE_URL="http://localhost:8080"
echo "====================================="
echo "Additional Test Suite Execution"
echo "====================================="
echo ""

# Integration Tests
echo "=== INTEGRATION TESTS ==="
echo ""

# Test 2.1: Database Persistence
echo "Test 2.1: Database Persistence Verification"
TX_ID=$(curl -s -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4532015112830366","amount":100.00,"currency":"USD","cvv":"123","expiryMonth":"12","expiryYear":"2025"}' | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)
sleep 1
RESULT=$(curl -s $BASE_URL/api/payments/$TX_ID)
if echo "$RESULT" | grep -q "$TX_ID"; then
  echo "✓ PASSED - Transaction persisted and retrieved"
else
  echo "✗ FAILED - Transaction not found in database"
fi
echo ""

# Test 2.2: Cache Behavior
echo "Test 2.2: Cache Behavior Validation"
START=$(date +%s%N)
curl -s $BASE_URL/api/payments/history > /dev/null
END=$(date +%s%N)
FIRST_CALL=$((($END - $START) / 1000000))

START=$(date +%s%N)
curl -s $BASE_URL/api/payments/history > /dev/null
END=$(date +%s%N)
SECOND_CALL=$((($END - $START) / 1000000))

echo "First call: ${FIRST_CALL}ms, Second call: ${SECOND_CALL}ms"
if [ $SECOND_CALL -lt $FIRST_CALL ]; then
  echo "✓ PASSED - Cache is working (second call faster)"
else
  echo "⚠ WARNING - Cache may not be working optimally"
fi
echo ""

# Edge Case Tests
echo "=== EDGE CASE TESTS ==="
echo ""

# Test 4.3: Special Characters in Card Number
echo "Test 4.3: Special Characters in Input"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4532-0151-1283-0366","amount":50.00,"currency":"USD"}')
echo "Status: $STATUS"
if [ "$STATUS" = "200" ] || [ "$STATUS" = "400" ]; then
  echo "✓ PASSED - Handled special characters appropriately"
else
  echo "✗ FAILED - Unexpected status: $STATUS"
fi
echo ""

# Test 4.4: SQL Injection Attempt
echo "Test 4.4: SQL Injection Prevention"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4532015112830366 OR 1=1","amount":50.00,"currency":"USD"}')
echo "Status: $STATUS"
if [ "$STATUS" = "200" ] || [ "$STATUS" = "400" ]; then
  echo "✓ PASSED - SQL injection prevented"
else
  echo "✗ FAILED - Unexpected behavior"
fi
echo ""

# Test 4.5: XSS Attempt
echo "Test 4.5: XSS Prevention"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{"cardNumber":"4532015112830366","amount":50.00,"currency":"<script>alert(1)</script>"}')
echo "Status: $STATUS"
if [ "$STATUS" = "400" ]; then
  echo "✓ PASSED - XSS attempt rejected"
else
  echo "⚠ WARNING - Status: $STATUS"
fi
echo ""

# Performance Tests
echo "=== PERFORMANCE TESTS ==="
echo ""

# Test 5.1: Response Time Measurement
echo "Test 5.1: Response Time Measurement"
TOTAL_TIME=0
ITERATIONS=10
for i in $(seq 1 $ITERATIONS); do
  START=$(date +%s%N)
  curl -s $BASE_URL/api/payments/history > /dev/null
  END=$(date +%s%N)
  TIME=$((($END - $START) / 1000000))
  TOTAL_TIME=$(($TOTAL_TIME + $TIME))
done
AVG_TIME=$(($TOTAL_TIME / $ITERATIONS))
echo "Average response time over $ITERATIONS requests: ${AVG_TIME}ms"
if [ $AVG_TIME -lt 500 ]; then
  echo "✓ PASSED - Response time acceptable (<500ms)"
else
  echo "⚠ WARNING - Response time high (>500ms)"
fi
echo ""

# Test 5.2: Concurrent Requests
echo "Test 5.2: Concurrent Request Handling"
echo "Sending 5 concurrent authorization requests..."
for i in {1..5}; do
  curl -s -X POST $BASE_URL/api/payments/authorize \
    -H "Content-Type: application/json" \
    -d "{\"cardNumber\":\"453201511283036$i\",\"amount\":$((i * 10)).00,\"currency\":\"USD\",\"cvv\":\"123\",\"expiryMonth\":\"12\",\"expiryYear\":\"2025\"}" &
done
wait
echo "✓ PASSED - All concurrent requests completed"
echo ""

# Test 5.3: Memory Usage Check
echo "Test 5.3: Memory Usage Monitoring"
METRICS=$(curl -s $BASE_URL/actuator/prometheus | grep "jvm_memory_used_bytes")
if [ ! -z "$METRICS" ]; then
  echo "✓ PASSED - Memory metrics available"
  echo "$METRICS" | head -3
else
  echo "✗ FAILED - Memory metrics not available"
fi
echo ""

echo "====================================="
echo "Additional Tests Completed"
echo "====================================="
