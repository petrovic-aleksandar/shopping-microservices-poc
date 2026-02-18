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

Write-Host 'Starting infrastructure (Postgres, Zookeeper, Kafka, Kafka UI)...' -ForegroundColor Cyan
docker compose up -d

$containers = @('shopping-postgres', 'shopping-zookeeper', 'shopping-kafka', 'shopping-kafka-ui')
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
        exit 0
    }

    Start-Sleep -Seconds $delaySeconds
}

Write-Host 'Infrastructure did not become fully ready in time. Check container logs:' -ForegroundColor Yellow
Write-Host 'docker compose logs --tail=100' -ForegroundColor Yellow
exit 1
