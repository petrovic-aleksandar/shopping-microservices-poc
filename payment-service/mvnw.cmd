@ECHO OFF
setlocal
set SCRIPT_DIR=%~dp0
if "%SCRIPT_DIR:~-1%"=="\" set SCRIPT_DIR=%SCRIPT_DIR:~0,-1%

where mvn >NUL 2>NUL
if %ERRORLEVEL% EQU 0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

where docker >NUL 2>NUL
if NOT %ERRORLEVEL% EQU 0 (
  echo Neither Maven nor Docker is installed. Install one of them and retry.
  exit /b 1
)

docker run --rm -v "%SCRIPT_DIR%":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn %*
exit /b %ERRORLEVEL%
