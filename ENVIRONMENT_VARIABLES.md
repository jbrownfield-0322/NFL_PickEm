# Environment Variables Configuration

This document lists all configurable environment variables for the NFL Pickem application.

## Database Configuration

### Railway Deployment (Primary)
- `DATABASE_URL` - Railway's PostgreSQL connection string (automatically provided by Railway)

### Local Development (Fallback)
- `DB_HOST` - Database host (default: localhost)
- `DB_PORT` - Database port (default: 5432)
- `DB_NAME` - Database name (default: nflpickem)
- `DB_USER` - Database username (default: postgres)
- `DB_PASSWORD` - Database password (default: empty)

## NFL Configuration
- `NFL_SEASON_WEEKS` - Number of weeks in the NFL regular season (default: 18)

## Feature Flags
- `ENABLE_NFL_SCRAPING` - Enable automatic NFL schedule scraping (default: false)

## Security Configuration (Railway)
- `ADMIN_USERNAME` - Admin username for basic auth (disabled by default)
- `ADMIN_PASSWORD` - Admin password for basic auth (disabled by default)

## Server Configuration
- `PORT` - Server port (default: 8080, Railway sets this automatically)

## Frontend Configuration
- `REACT_APP_API_URL` - API base URL for frontend (default: /api in production, http://localhost:8080 in development)

## Example Local Development Setup

Create a `.env` file in the root directory:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nflpickem
DB_USER=postgres
DB_PASSWORD=your_password

# NFL Configuration
NFL_SEASON_WEEKS=18

# Features
ENABLE_NFL_SCRAPING=true

# Frontend
REACT_APP_API_URL=http://localhost:8080
```

## Railway Deployment

Railway automatically provides:
- `DATABASE_URL` - PostgreSQL connection string
- `PORT` - Server port

You can optionally set:
- `NFL_SEASON_WEEKS` - Override default 18-week season
- `ENABLE_NFL_SCRAPING` - Enable automatic schedule scraping
