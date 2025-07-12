#!/bin/bash

echo "=== Payment Summary Performance Test ==="
echo "Starting backend services..."

# Ensure services are running
#docker-compose up -d

echo "Waiting for services to be ready..."
#sleep 10

echo "Running performance test..."
k6 run summary-performance-test.js

echo "=== Test completed ==="
echo "Check the output above for:"
echo "- Population phase: ~5000 payments created"
echo "- Test phase: Summary queries with different intervals"
echo "- Metrics: p99 and average response times for each interval"