#!/bin/bash

echo "🛑 Stopping infrastructure services..."

# Define the infrastructure containers
containers=(
    "shopping-postgres"
    "shopping-zookeeper"
    "shopping-kafka"
    "shopping-kafka-ui"
    "shopping-elasticsearch"
    "shopping-kibana"
    "shopping-filebeat"
    "shopping-prometheus"
    "shopping-grafana"
    "shopping-alertmanager"
)

# Stop each container
for container in "${containers[@]}"; do
    if docker ps -q -f name="$container" | grep -q .; then
        echo "Stopping $container..."
        docker stop "$container"
    else
        echo "$container is not running"
    fi
done

echo ""
echo "✅ Infrastructure services stopped"
echo ""
echo "💡 Resources freed on your laptop"
echo "   Run './start-infra.sh' when you need to resume development"
