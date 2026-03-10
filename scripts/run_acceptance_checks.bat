@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0run_acceptance_checks.ps1" %*
set EXIT_CODE=%ERRORLEVEL%
endlocal & exit /b %EXIT_CODE%
