# NFL Pick'em Scraper Runner for Windows PowerShell
# This script makes it easy to run the NFL scraper scripts

param(
    [Parameter(Position=0)]
    [string]$Command = "help",
    
    [Parameter(Position=1)]
    [string]$Option = ""
)

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Blue"
$White = "White"

# Function to print colored output
function Write-Status {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor $Blue
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor $Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor $Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor $Red
}

# Function to check if command exists
function Test-Command {
    param([string]$CommandName)
    return [bool](Get-Command $CommandName -ErrorAction SilentlyContinue)
}

# Function to check environment variables
function Test-Environment {
    if (-not $env:API_BASE) {
        Write-Error "API_BASE environment variable is not set"
        Write-Status "Please set it to your API URL (e.g., https://your-app.railway.app/api)"
        Write-Status "You can set it with: `$env:API_BASE = 'https://your-app.railway.app/api'"
        exit 1
    }
    
    Write-Success "Environment variables are set correctly"
    Write-Status "API_BASE: $env:API_BASE"
    Write-Status "SEASON: $(if ($env:SEASON) { $env:SEASON } else { '2024' })"
}

# Function to install dependencies
function Install-Dependencies {
    Write-Status "Checking dependencies..."
    
    if (-not (Test-Command "node")) {
        Write-Error "Node.js is not installed"
        Write-Status "Please install Node.js from https://nodejs.org/"
        exit 1
    }
    
    if (-not (Test-Command "npm")) {
        Write-Error "npm is not installed"
        Write-Status "Please install npm"
        exit 1
    }
    
    if (-not (Test-Path "package.json")) {
        Write-Error "package.json not found"
        Write-Status "Please run this script from the scripts directory"
        exit 1
    }
    
    Write-Status "Installing npm dependencies..."
    npm install
    
    Write-Success "Dependencies installed successfully"
}

# Function to scrape schedule
function Invoke-ScheduleScraper {
    param([string]$Option)
    
    Write-Status "Starting NFL schedule scraper..."
    
    if ($Option -eq "--from-file") {
        Write-Status "Loading schedule from file..."
        node scrape-nfl-schedule.js --from-file
    } else {
        Write-Status "Scraping schedule from Pro-Football-Reference.com..."
        node scrape-nfl-schedule.js
    }
    
    Write-Success "Schedule scraping completed"
}

# Function to scrape scores
function Invoke-ScoreScraper {
    Write-Status "Starting NFL score scraper..."
    node scrape-nfl-scores.js
    Write-Success "Score scraping completed"
}

# Function to load sample games
function Invoke-SampleLoader {
    Write-Status "Loading sample games..."
    node ../add-sample-games.js
    Write-Success "Sample games loaded"
}

# Function to show help
function Show-Help {
    Write-Host "NFL Pick'em Scraper Runner for Windows" -ForegroundColor $White
    Write-Host ""
    Write-Host "Usage: .\run-scraper.ps1 [COMMAND] [OPTIONS]" -ForegroundColor $White
    Write-Host ""
    Write-Host "Commands:" -ForegroundColor $White
    Write-Host "  schedule [--from-file]  Scrape and load NFL schedule" -ForegroundColor $White
    Write-Host "  scores                  Scrape and update game scores" -ForegroundColor $White
    Write-Host "  samples                 Load sample games for testing" -ForegroundColor $White
    Write-Host "  install                 Install dependencies" -ForegroundColor $White
    Write-Host "  check                   Check environment and dependencies" -ForegroundColor $White
    Write-Host "  help                    Show this help message" -ForegroundColor $White
    Write-Host ""
    Write-Host "Options:" -ForegroundColor $White
    Write-Host "  --from-file             Load schedule from existing file instead of scraping" -ForegroundColor $White
    Write-Host ""
    Write-Host "Environment Variables:" -ForegroundColor $White
    Write-Host "  API_BASE                Your API base URL (required)" -ForegroundColor $White
    Write-Host "  SEASON                  NFL season year (default: 2024)" -ForegroundColor $White
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor $White
    Write-Host "  .\run-scraper.ps1 schedule             # Scrape and load complete schedule" -ForegroundColor $White
    Write-Host "  .\run-scraper.ps1 schedule --from-file # Load schedule from file" -ForegroundColor $White
    Write-Host "  .\run-scraper.ps1 scores               # Update game scores" -ForegroundColor $White
    Write-Host "  .\run-scraper.ps1 samples              # Load sample games" -ForegroundColor $White
    Write-Host ""
    Write-Host "Setting Environment Variables:" -ForegroundColor $White
    Write-Host "  `$env:API_BASE = 'https://your-app.railway.app/api'" -ForegroundColor $White
    Write-Host "  `$env:SEASON = '2024'" -ForegroundColor $White
}

# Main script logic
function Main {
    # Check if we're in the right directory
    if (-not (Test-Path "scrape-nfl-schedule.js")) {
        Write-Error "This script must be run from the scripts directory"
        exit 1
    }
    
    # Parse command line arguments
    switch ($Command.ToLower()) {
        "schedule" {
            Test-Environment
            Install-Dependencies
            Invoke-ScheduleScraper -Option $Option
        }
        "scores" {
            Test-Environment
            Install-Dependencies
            Invoke-ScoreScraper
        }
        "samples" {
            Test-Environment
            Install-Dependencies
            Invoke-SampleLoader
        }
        "install" {
            Install-Dependencies
        }
        "check" {
            Test-Environment
            Install-Dependencies
            Write-Success "All checks passed"
        }
        "help" {
            Show-Help
        }
        default {
            Write-Error "Unknown command: $Command"
            Write-Host ""
            Show-Help
            exit 1
        }
    }
}

# Run main function
Main
