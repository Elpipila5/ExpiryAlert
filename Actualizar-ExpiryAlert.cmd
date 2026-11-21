@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\Actualizar-ExpiryAlert.ps1" %*
exit /b %errorlevel%
