$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host 'Checking Docker availability...' -ForegroundColor Cyan
try {
    docker info | Out-Null
} catch {
    Write-Host 'Docker is not running. Start Docker Desktop and retry.' -ForegroundColor Red
    exit 1
}

Write-Host 'Starting infrastructure (Postgres, Zookeeper, Kafka, Kafka UI, ELK Stack, Prometheus, Grafana, Alertmanager)...' -ForegroundColor Cyan
docker compose up -d

$containers = @('shopping-postgres', 'shopping-zookeeper', 'shopping-kafka', 'shopping-kafka-ui', 'shopping-elasticsearch', 'shopping-kibana', 'shopping-filebeat', 'shopping-prometheus', 'shopping-grafana', 'shopping-alertmanager')
$maxAttempts = 20
$delaySeconds = 3

Write-Host 'Waiting for containers to reach running state...' -ForegroundColor Cyan
for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    $running = @()
    foreach ($name in $containers) {
        $state = docker inspect -f "{{.State.Running}}" $name 2>$null
        if ($state -eq 'true') {
            $running += $name
        }
    }

    if ($running.Count -eq $containers.Count) {
        Write-Host 'Infrastructure is up.' -ForegroundColor Green
        Write-Host 'Kafka UI: http://localhost:8085' -ForegroundColor Green
        Write-Host 'Kibana: http://localhost:5601' -ForegroundColor Green
        Write-Host 'Elasticsearch: http://localhost:9200' -ForegroundColor Green
        Write-Host 'Prometheus: http://localhost:9090' -ForegroundColor Green
        Write-Host 'Grafana: http://localhost:3000' -ForegroundColor Green
        Write-Host 'Alertmanager: http://localhost:9093' -ForegroundColor Green
        exit 0
    }

    Start-Sleep -Seconds $delaySeconds
}

Write-Host 'Infrastructure did not become fully ready in time. Check container logs:' -ForegroundColor Yellow
Write-Host 'docker compose logs --tail=100' -ForegroundColor Yellow
exit 1
