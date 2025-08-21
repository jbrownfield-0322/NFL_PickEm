@echo off
REM NFL Pick'em Scraper Runner for Windows
REM This script makes it easy to run the NFL scraper scripts

setlocal enabledelayedexpansion

REM Check if we're in the right directory
if not exist "scrape-nfl-schedule.js" (
    echo [ERROR] This script must be run from the scripts directory
    exit /b 1
)

REM Get command from first argument
set "command=%1"
if "%command%"=="" set "command=help"

REM Get option from second argument
set "option=%2"

REM Check environment variables
if "%API_BASE%"=="" (
    echo [ERROR] API_BASE environment variable is not set
    echo [INFO] Please set it to your API URL (e.g., https://your-app.railway.app/api)
    echo [INFO] You can set it with: set API_BASE=https://your-app.railway.app/api
    exit /b 1
)

echo [INFO] Environment variables are set correctly
echo [INFO] API_BASE: %API_BASE%
echo [INFO] SEASON: %SEASON%

REM Check if Node.js is installed
node --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not installed
    echo [INFO] Please install Node.js from https://nodejs.org/
    exit /b 1
)

REM Check if npm is installed
npm --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm is not installed
    echo [INFO] Please install npm
    exit /b 1
)

REM Install dependencies
echo [INFO] Installing npm dependencies...
npm install
if errorlevel 1 (
    echo [ERROR] Failed to install dependencies
    exit /b 1
)

echo [SUCCESS] Dependencies installed successfully

REM Execute commands
if /i "%command%"=="schedule" (
    echo [INFO] Starting NFL schedule scraper...
    if /i "%option%"=="--from-file" (
        echo [INFO] Loading schedule from file...
        node scrape-nfl-schedule.js --from-file
    ) else (
        echo [INFO] Scraping schedule from Pro-Football-Reference.com...
        node scrape-nfl-schedule.js
    )
    echo [SUCCESS] Schedule scraping completed
) else if /i "%command%"=="scores" (
    echo [INFO] Starting NFL score scraper...
    node scrape-nfl-scores.js
    echo [SUCCESS] Score scraping completed
) else if /i "%command%"=="samples" (
    echo [INFO] Loading sample games...
    node ../add-sample-games.js
    echo [SUCCESS] Sample games loaded
) else if /i "%command%"=="install" (
    echo [SUCCESS] Dependencies already installed
) else if /i "%command%"=="check" (
    echo [SUCCESS] All checks passed
) else if /i "%command%"=="help" (
    echo NFL Pick'em Scraper Runner for Windows
    echo.
    echo Usage: run-scraper.bat [COMMAND] [OPTIONS]
    echo.
    echo Commands:
    echo   schedule [--from-file]  Scrape and load NFL schedule
    echo   scores                  Scrape and update game scores
    echo   samples                 Load sample games for testing
    echo   install                 Install dependencies
    echo   check                   Check environment and dependencies
    echo   help                    Show this help message
    echo.
    echo Options:
    echo   --from-file             Load schedule from existing file instead of scraping
    echo.
    echo Environment Variables:
    echo   API_BASE                Your API base URL (required)
    echo   SEASON                  NFL season year (default: 2024)
    echo.
    echo Examples:
    echo   run-scraper.bat schedule             # Scrape and load complete schedule
    echo   run-scraper.bat schedule --from-file # Load schedule from file
    echo   run-scraper.bat scores               # Update game scores
    echo   run-scraper.bat samples              # Load sample games
    echo.
    echo Setting Environment Variables:
    echo   set API_BASE=https://your-app.railway.app/api
    echo   set SEASON=2024
) else (
    echo [ERROR] Unknown command: %command%
    echo.
    call :help
    exit /b 1
)

exit /b 0

:help
echo NFL Pick'em Scraper Runner for Windows
echo.
echo Usage: run-scraper.bat [COMMAND] [OPTIONS]
echo.
echo Commands:
echo   schedule [--from-file]  Scrape and load NFL schedule
echo   scores                  Scrape and update game scores
echo   samples                 Load sample games for testing
echo   install                 Install dependencies
echo   check                   Check environment and dependencies
echo   help                    Show this help message
echo.
echo Options:
echo   --from-file             Load schedule from existing file instead of scraping
echo.
echo Environment Variables:
echo   API_BASE                Your API base URL (required)
echo   SEASON                  NFL season year (default: 2024)
echo.
echo Examples:
echo   run-scraper.bat schedule             # Scrape and load complete schedule
echo   run-scraper.bat schedule --from-file # Load schedule from file
echo   run-scraper.bat scores               # Update game scores
echo   run-scraper.bat samples              # Load sample games
echo.
echo Setting Environment Variables:
echo   set API_BASE=https://your-app.railway.app/api
echo   set SEASON=2024
goto :eof
