@echo off
setlocal

set SCRIPT_DIR=%~dp0
powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run_backend_checks.ps1" %*

if errorlevel 1 (
  echo Backend checks failed.
  exit /b 1
)

echo Backend checks completed.
exit /b 0
