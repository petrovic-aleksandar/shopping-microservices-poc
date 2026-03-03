#!/bin/bash

# Clear all logs across the microservices project
# Usage: ./clear-logs.sh

echo "Clearing all logs..."

# Clear logs in service directories
find . -path "*/logs/*.log" -exec truncate -s 0 {} \;

# Confirm completion
echo "✓ All logs cleared successfully"
