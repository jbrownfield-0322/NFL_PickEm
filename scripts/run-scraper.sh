#!/bin/bash

# NFL Pick'em Scraper Runner
# This script makes it easy to run the NFL scraper scripts

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}
1
# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&
}

# Function to check environment variables
check_env() {
    if [ -z "$API_BASE" ]; then
        print_error "API_BASE environment variable is not set"
        print_status "Please set it to your API URL (e.g., https://your-app.railway.app/api)"
        exit 1
    fi
    
    print_success "Environment variables are set correctly"
    print_status "API_BASE: $API_BASE"
    print_status "SEASON: ${SEASON:-2024}"
}

# Function to install dependencies
install_deps() {
    print_status "Checking dependencies..."
    
    if ! command_exists node; then
        print_error "Node.js is not installed"
        print_status "Please install Node.js from https://nodejs.org/"
        exit 1
    fi
    
    if ! command_exists npm; then
        print_error "npm is not installed"
        print_status "Please install npm"
        exit 1
    fi
    
    if [ ! -f "package.json" ]; then
        print_error "package.json not found"
        print_status "Please run this script from the scripts directory"
        exit 1
    fi
    
    print_status "Installing npm dependencies..."
    npm install
    
    print_success "Dependencies installed successfully"
}

# Function to scrape schedule
scrape_schedule() {
    print_status "Starting NFL schedule scraper..."
    
    if [ "$1" = "--from-file" ]; then
        print_status "Loading schedule from file..."
        node scrape-nfl-schedule.js --from-file
    else
        print_status "Scraping schedule from Pro-Football-Reference.com..."
        node scrape-nfl-schedule.js
    fi
    
    print_success "Schedule scraping completed"
}

# Function to scrape scores
scrape_scores() {
    print_status "Starting NFL score scraper..."
    node scrape-nfl-scores.js
    print_success "Score scraping completed"
}

# Function to load sample games
load_samples() {
    print_status "Loading sample games..."
    node ../add-sample-games.js
    print_success "Sample games loaded"
}

# Function to show help
show_help() {
    echo "NFL Pick'em Scraper Runner"
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  schedule [--from-file]  Scrape and load NFL schedule"
    echo "  scores                  Scrape and update game scores"
    echo "  samples                 Load sample games for testing"
    echo "  install                 Install dependencies"
    echo "  check                   Check environment and dependencies"
    echo "  help                    Show this help message"
    echo ""
    echo "Options:"
    echo "  --from-file             Load schedule from existing file instead of scraping"
    echo ""
    echo "Environment Variables:"
    echo "  API_BASE                Your API base URL (required)"
    echo "  SEASON                  NFL season year (default: 2024)"
    echo ""
    echo "Examples:"
    echo "  $0 schedule             # Scrape and load complete schedule"
    echo "  $0 schedule --from-file # Load schedule from file"
    echo "  $0 scores               # Update game scores"
    echo "  $0 samples              # Load sample games"
}

# Main script logic
main() {
    # Check if we're in the right directory
    if [ ! -f "scrape-nfl-schedule.js" ]; then
        print_error "This script must be run from the scripts directory"
        exit 1
    fi
    
    # Parse command line arguments
    case "${1:-help}" in
        "schedule")
            check_env
            install_deps
            scrape_schedule "$2"
            ;;
        "scores")
            check_env
            install_deps
            scrape_scores
            ;;
        "samples")
            check_env
            install_deps
            load_samples
            ;;
        "install")
            install_deps
            ;;
        "check")
            check_env
            install_deps
            print_success "All checks passed"
            ;;
        "help"|"--help"|"-h")
            show_help
            ;;
        *)
            print_error "Unknown command: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
