@echo off
REM Starts CivicDesk on http://localhost:8080 (Windows)
cd /d "%~dp0"
if "%DB_PASSWORD%"=="" set /p DB_PASSWORD=MySQL password for root: 
call mvnw.cmd spring-boot:run
