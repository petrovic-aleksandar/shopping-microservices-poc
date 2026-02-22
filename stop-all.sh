#!/usr/bin/env bash
set -euo pipefail

echo "Stopping Java service processes started by spring-boot:run..."
if pkill -f "spring-boot:run|org.springframework.boot.loader"; then
  echo "Stopped matching Java processes."
else
  echo "No matching Java processes found."
fi

echo "Done."
