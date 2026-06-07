#!/bin/bash

# Payment Application Test Execution Script
# This script runs comprehensive end-to-end tests

BASE_URL="http://localhost:8080"
TEST_RESULTS_FILE="test_results.json"

echo "==================================="
echo "Payment Application Test Suite"
echo "==================================="
echo ""

# Initialize results
echo "{\"tests\": [" > $TEST_RESULTS_FILE

# Test 1.1.1: Successful Authorization
echo "Test 1.1.1: Successful Payment Authorization"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "amount": 100.00,
    "currency": "USD",
    "cvv": "123",
    "expiryMonth": "12",
    "expiryYear": "2025"
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
TRANSACTION_ID=$(echo "$BODY" | grep -o '"transactionId":"[^"]*"' | cut -d'"' -f4)

if [ "$HTTP_CODE" = "200" ] && [ ! -z "$TRANSACTION_ID" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE, Transaction ID: $TRANSACTION_ID"
    TEST_1_1_1="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_1_1="FAILED"
fi
echo ""

# Test 1.1.2: Authorization with Minimum Amount
echo "Test 1.1.2: Authorization with Minimum Amount"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "amount": 0.01,
    "currency": "USD",
    "cvv": "123",
    "expiryMonth": "12",
    "expiryYear": "2025"
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE"
    TEST_1_1_2="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_1_2="FAILED"
fi
echo ""

# Test 1.2.1: Successful Capture
echo "Test 1.2.1: Successful Payment Capture"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/capture \
  -H "Content-Type: application/json" \
  -d "{
    \"transactionId\": \"$TRANSACTION_ID\",
    \"cardNumber\": \"4111111111111111\",
    \"amount\": 100.00,
    \"currency\": \"USD\"
  }" \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q "CAPTURED"; then
    echo "✓ PASSED - Status: $HTTP_CODE, Transaction captured"
    TEST_1_2_1="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_2_1="FAILED"
fi
echo ""

# Test 1.2.2: Capture Non-Existent Transaction
echo "Test 1.2.2: Capture Non-Existent Transaction"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/capture \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "non-existent-id-12345",
    "cardNumber": "4111111111111111",
    "amount": 100.00,
    "currency": "USD"
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "400" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE (Expected 400)"
    TEST_1_2_2="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE (Expected 400)"
    TEST_1_2_2="FAILED"
fi
echo ""

# Test 1.3.1: Successful Refund
echo "Test 1.3.1: Successful Payment Refund"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/refund \
  -H "Content-Type: application/json" \
  -d "{
    \"transactionId\": \"$TRANSACTION_ID\",
    \"cardNumber\": \"4111111111111111\",
    \"amount\": 100.00,
    \"currency\": \"USD\"
  }" \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q "REFUNDED"; then
    echo "✓ PASSED - Status: $HTTP_CODE, Transaction refunded"
    TEST_1_3_1="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_3_1="FAILED"
fi
echo ""

# Test 1.4.1: Get Transaction History
echo "Test 1.4.1: Get Transaction History"
RESPONSE=$(curl -X GET $BASE_URL/api/payments/history \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q "id"; then
    echo "✓ PASSED - Status: $HTTP_CODE, History retrieved"
    TEST_1_4_1="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_4_1="FAILED"
fi
echo ""

# Test 1.4.2: Get Specific Transaction
echo "Test 1.4.2: Get Specific Transaction by ID"
RESPONSE=$(curl -X GET $BASE_URL/api/payments/$TRANSACTION_ID \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE"
    TEST_1_4_2="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_4_2="FAILED"
fi
echo ""

# Test 1.4.3: Get Non-Existent Transaction
echo "Test 1.4.3: Get Non-Existent Transaction"
RESPONSE=$(curl -X GET $BASE_URL/api/payments/invalid-id-12345 \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "404" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE (Expected 404)"
    TEST_1_4_3="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE (Expected 404)"
    TEST_1_4_3="FAILED"
fi
echo ""

# Test 1.5.1: Health Check
echo "Test 1.5.1: Health Check"
RESPONSE=$(curl -X GET $BASE_URL/actuator/health \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q "UP"; then
    echo "✓ PASSED - Status: $HTTP_CODE, Health: UP"
    TEST_1_5_1="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_5_1="FAILED"
fi
echo ""

# Test 1.5.2: Clear Cache
echo "Test 1.5.2: Clear Cache"
RESPONSE=$(curl -X POST $BASE_URL/admin/cache/clear \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q "success"; then
    echo "✓ PASSED - Status: $HTTP_CODE, Cache cleared"
    TEST_1_5_2="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_5_2="FAILED"
fi
echo ""

# Test 1.5.3: Prometheus Metrics
echo "Test 1.5.3: Prometheus Metrics"
RESPONSE=$(curl -X GET $BASE_URL/actuator/prometheus \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE"
    TEST_1_5_3="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_1_5_3="FAILED"
fi
echo ""

# Test 3.1.1: Missing Card Number
echo "Test 3.1.1: Missing Card Number"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "currency": "USD"
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "400" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_1_1="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_1_1="FAILED"
fi
echo ""

# Test 3.1.2: Negative Amount
echo "Test 3.1.2: Negative Amount"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "amount": -50.00,
    "currency": "USD"
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "400" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_1_2="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_1_2="FAILED"
fi
echo ""

# Test 3.1.3: Zero Amount
echo "Test 3.1.3: Zero Amount"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "amount": 0.00,
    "currency": "USD"
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "400" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_1_3="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_1_3="FAILED"
fi
echo ""

# Test 3.1.4: Missing Currency
echo "Test 3.1.4: Missing Currency"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "amount": 100.00
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "400" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_1_4="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_1_4="FAILED"
fi
echo ""

# Test 3.2.1: Capture Without Transaction ID
echo "Test 3.2.1: Capture Without Transaction ID"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/capture \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "amount": 100.00,
    "currency": "USD"
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "400" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_2_1="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE (Expected 400)"
    TEST_3_2_1="FAILED"
fi
echo ""

# Test 4.2.1: Large Amount
echo "Test 4.2.1: Large Transaction Amount"
RESPONSE=$(curl -X POST $BASE_URL/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "amount": 999999999.99,
    "currency": "USD",
    "cvv": "123",
    "expiryMonth": "12",
    "expiryYear": "2025"
  }' \
  -w "\n%{http_code}" \
  -s)

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ PASSED - Status: $HTTP_CODE"
    TEST_4_2_1="PASSED"
else
    echo "✗ FAILED - Status: $HTTP_CODE"
    TEST_4_2_1="FAILED"
fi
echo ""

# Summary
echo "==================================="
echo "Test Execution Summary"
echo "==================================="
PASSED=0
FAILED=0

for test in TEST_1_1_1 TEST_1_1_2 TEST_1_2_1 TEST_1_2_2 TEST_1_3_1 TEST_1_4_1 TEST_1_4_2 TEST_1_4_3 TEST_1_5_1 TEST_1_5_2 TEST_1_5_3 TEST_3_1_1 TEST_3_1_2 TEST_3_1_3 TEST_3_1_4 TEST_3_2_1 TEST_4_2_1; do
    if [ "${!test}" = "PASSED" ]; then
        ((PASSED++))
    else
        ((FAILED++))
    fi
done

TOTAL=$((PASSED + FAILED))
echo "Total Tests: $TOTAL"
echo "Passed: $PASSED"
echo "Failed: $FAILED"
echo "Success Rate: $(awk "BEGIN {printf \"%.2f\", ($PASSED/$TOTAL)*100}")%"
echo ""

echo "Test execution completed!"

# Made with Bob
