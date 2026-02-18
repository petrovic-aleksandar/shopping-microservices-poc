$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$services = @(
    @{ Name = 'auth-service'; Port = 8081 },
    @{ Name = 'order-service'; Port = 8082 },
    @{ Name = 'payment-service'; Port = 8083 },
    @{ Name = 'inventory-service'; Port = 8084 },
    @{ Name = 'api-gateway'; Port = 8080 }
)

Write-Host 'Starting shopping microservices...' -ForegroundColor Cyan
Write-Host 'Tip: ensure Docker Desktop is running (for Kafka and Maven fallback).' -ForegroundColor Yellow

foreach ($service in $services) {
    $serviceDir = Join-Path $root $service.Name
    $wrapper = Join-Path $serviceDir 'mvnw.cmd'

    if (-not (Test-Path $serviceDir)) {
        Write-Host "Skipping $($service.Name): directory not found" -ForegroundColor Red
        continue
    }

    if (-not (Test-Path $wrapper)) {
        Write-Host "Skipping $($service.Name): mvnw.cmd not found" -ForegroundColor Red
        continue
    }

    $cmd = "cd /d `"$serviceDir`" && mvnw.cmd spring-boot:run"
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/k', $cmd | Out-Null
    Write-Host "Launched $($service.Name) on expected port $($service.Port)" -ForegroundColor Green
}

Write-Host ''
Write-Host 'All launch commands sent in separate terminals.' -ForegroundColor Cyan
Write-Host 'After services are up, open gateway at http://localhost:8080' -ForegroundColor Cyan
