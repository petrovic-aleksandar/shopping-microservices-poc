#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
services=(
  "auth-service:8081"
  "order-service:8082"
  "payment-service:8083"
  "inventory-service:8084"
  "api-gateway:8080"
)

log_dir="$root_dir/logs"
mkdir -p "$log_dir"

echo "Starting shopping microservices..."
echo "Tip: ensure Docker Desktop is running (for Kafka and Maven fallback)."

for entry in "${services[@]}"; do
  service_name="${entry%%:*}"
  service_port="${entry##*:}"
  service_dir="$root_dir/$service_name"
  wrapper="$service_dir/mvnw"

  if [[ ! -d "$service_dir" ]]; then
    echo "Skipping $service_name: directory not found"
    continue
  fi

  if [[ ! -f "$wrapper" ]]; then
    echo "Skipping $service_name: mvnw not found"
    continue
  fi

  (cd "$service_dir" && ./mvnw spring-boot:run >"$log_dir/$service_name.log" 2>&1 &)
  echo "Launched $service_name on expected port $service_port (log: logs/$service_name.log)"
done

echo ""
echo "All launch commands sent in background processes."
echo "After services are up, open gateway at http://localhost:8080"
