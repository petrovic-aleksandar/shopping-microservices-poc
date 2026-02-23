#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$root_dir"

echo "Checking Docker availability..."
if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running. Start Docker Desktop and retry."
  exit 1
fi

echo "Starting infrastructure (Postgres, Zookeeper, Kafka, Kafka UI, ELK Stack)..."
docker compose up -d

containers=(shopping-postgres shopping-zookeeper shopping-kafka shopping-kafka-ui shopping-elasticsearch shopping-kibana shopping-filebeat)
max_attempts=30
delay_seconds=5

echo "Waiting for containers to reach running state..."
for ((attempt=1; attempt<=max_attempts; attempt++)); do
  running=0
  for name in "${containers[@]}"; do
    state="$(docker inspect -f "{{.State.Running}}" "$name" 2>/dev/null || true)"
    if [[ "$state" == "true" ]]; then
      running=$((running + 1))
    fi
  done

  if [[ "$running" -eq "${#containers[@]}" ]]; then
    echo "Infrastructure is up."
    echo "Kafka UI: http://localhost:8085"
    echo "Kibana: http://localhost:5601"
    echo "Elasticsearch: http://localhost:9200"
    exit 0
  fi

  sleep "$delay_seconds"
done

echo "Infrastructure did not become fully ready in time. Check container logs:"
echo "docker compose logs --tail=100"
exit 1
