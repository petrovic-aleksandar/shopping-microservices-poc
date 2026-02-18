$ErrorActionPreference = 'SilentlyContinue'

Write-Host 'Stopping Java service processes started by spring-boot:run...' -ForegroundColor Cyan
Get-CimInstance Win32_Process |
    Where-Object { $_.Name -match 'java.exe|javaw.exe' -and $_.CommandLine -match 'spring-boot:run|org.springframework.boot.loader' } |
    ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force
        Write-Host "Stopped PID $($_.ProcessId)" -ForegroundColor Yellow
    }

Write-Host 'Done.' -ForegroundColor Green
