# Odds API Implementation

This document describes the implementation of The Odds API integration for the NFL Pickem application.

## Overview

The odds API integration allows the application to fetch and display **FanDuel point spreads only** for NFL games. **Importantly, the system can also create and update games automatically based on the odds API data, ensuring the schedule is always current including flexed games.**

## Components

### 1. BettingOdds Model (`src/main/java/com/nflpickem/pickem/model/BettingOdds.java`)

The `BettingOdds` entity stores betting information for each game:

- **Game Relationship**: Links to the `Game` entity
- **Sportsbook**: The name of the sportsbook (FanDuel only)
- **Spread**: Point spread for the game
- **Spread Team**: Team that the spread applies to
- **Odds Type**: Format of odds (american, decimal, fractional)
- **Last Updated**: Timestamp of when odds were last fetched

### 2. BettingOddsRepository (`src/main/java/com/nflpickem/pickem/repository/BettingOddsRepository.java`)

Repository interface with custom queries for:
- Finding odds by game
- Finding odds by week
- Finding odds by sportsbook
- Finding stale odds for cleanup

### 3. OddsService (`src/main/java/com/nflpickem/pickem/service/OddsService.java`)

Core service that handles:
- **API Integration**: Fetches odds from The Odds API
- **Data Processing**: Maps API responses to internal models
- **Team Matching**: Matches API team names with database team names
- **Update Management**: Handles updating existing odds vs creating new ones
- **Stale Data Cleanup**: Removes old odds data

#### Key Methods:
- `fetchOddsForWeek(Integer week)`: Fetches odds from API for a specific week
- `fetchAllAvailableOdds()`: Fetches all available odds and creates/updates games as needed
- `updateOddsForWeek(Integer week)`: Updates or creates odds for a week
- `updateAllAvailableOdds()`: Updates all available odds and creates/updates games
- `getOddsForGame(Long gameId)`: Retrieves odds for a specific game
- `needsUpdate(Integer week)`: Checks if odds need updating
- `cleanupStaleOdds()`: Removes old odds data

### 4. Controllers

#### OddsController (`src/main/java/com/nflpickem/pickem/controller/OddsController.java`)
REST endpoints for odds management:
- `GET /api/odds/week/{week}`: Get odds for a week
- `GET /api/odds/game/{gameId}`: Get odds for a game
- `POST /api/odds/update/week/{week}`: Update odds for a week
- `POST /api/odds/update/all`: Update all available odds and create/update games
- `GET /api/odds/needs-update/{week}`: Check if odds need updating
- `POST /api/odds/cleanup`: Clean up stale odds

#### AdminController (`src/main/java/com/nflpickem/pickem/controller/AdminController.java`)
Admin endpoints for odds management:
- `POST /api/admin/odds/trigger-update`: Update odds for current week
- `POST /api/admin/odds/trigger-update/{week}`: Update odds for specific week
- `POST /api/admin/odds/trigger-update-all`: Update all available odds and create/update games
- `GET /api/admin/odds/status`: Check API configuration status

#### GameController (Updated)
Added odds endpoints:
- `GET /api/games/{gameId}/odds`: Get odds for a specific game
- `GET /api/games/week/{weekNum}/odds`: Get odds for a specific week

### 5. Scheduled Service (`src/main/java/com/nflpickem/pickem/service/ScheduledOddsService.java`)

Automated odds and games updates:
- **Every 6 hours**: Updates all available odds and creates/updates games during NFL season
- **Daily at 2 AM**: Cleans up stale odds data
- **Season Detection**: Only runs during NFL season (September-January)
- **Game Creation**: Automatically creates new games from odds API data
- **Game Updates**: Updates existing game times for flexed games

### 6. Configuration (`src/main/java/com/nflpickem/pickem/config/SchedulingConfig.java`)

Enables Spring's scheduling functionality for automated updates.

## Configuration

### Environment Variables

Set these in your Railway project or local environment:

```bash
# Required: Your Odds API key from https://the-odds-api.com/
ODDS_API_KEY=your_api_key_here

# Optional: API base URL (defaults to https://api.the-odds-api.com/v4)
ODDS_API_BASE_URL=https://api.the-odds-api.com/v4

# Optional: Update interval in hours (defaults to 6)
ODDS_UPDATE_INTERVAL_HOURS=6
```

### Application Properties

The following properties are configured in `application.properties` and `application-railway.properties`:

```properties
# Odds API Configuration
ODDS_API_KEY=${ODDS_API_KEY:}
ODDS_API_BASE_URL=${ODDS_API_BASE_URL:https://api.the-odds-api.com/v4}
ODDS_UPDATE_INTERVAL_HOURS=${ODDS_UPDATE_INTERVAL_HOURS:6}
```

## API Integration Details

### The Odds API

The service integrates with [The Odds API](https://the-odds-api.com/) which provides:
- **Point Spreads**: Betting lines for games
- **Totals**: Over/under point totals
- **Multiple Sportsbooks**: Data from various betting sites
- **Real-time Updates**: Current odds information

### API Endpoint Used

```
GET https://api.the-odds-api.com/v4/sports/americanfootball_nfl/odds/
```

**Parameters:**
- `apiKey`: Your API key
- `regions=us`: US betting markets
- `markets=spreads`: Point spreads only
- `bookmakers=fanduel`: FanDuel sportsbook only
- `oddsFormat=american`: American odds format
- `dateFormat=iso`: ISO date format

### Team Name Matching

The service normalizes team names to match API responses with database entries:
- Converts to lowercase
- Removes extra whitespace
- Handles variations in team names

### Game Creation and Updates

The enhanced service can now create and update games automatically:

#### Game Creation
- **New Games**: If no matching game exists, creates a new game from odds API data
- **Week Calculation**: Automatically determines the NFL week from game time
- **Complete Data**: Includes team names, kickoff time, and week information

#### Game Updates
- **Flexed Games**: Updates existing game times when they change (e.g., Sunday Night Football flexes)
- **Time Changes**: Compares current game time with API time and updates if different
- **Logging**: Logs all game creation and update activities

#### Week Determination
The service calculates NFL weeks based on:
- NFL season start (first Thursday of September)
- Game date comparison
- Automatic week assignment (1-18)

## Usage Examples

### Manual Odds and Games Update

```bash
# Update all available odds and create/update games
curl -X POST https://your-app.railway.app/api/admin/odds/trigger-update-all

# Update odds for current week only
curl -X POST https://your-app.railway.app/api/admin/odds/trigger-update

# Update odds for specific week only
curl -X POST https://your-app.railway.app/api/admin/odds/trigger-update/1
```

### Check API Status

```bash
curl https://your-app.railway.app/api/admin/odds/status
```

### Get Odds for Games

```bash
# Get odds for a specific game
curl https://your-app.railway.app/api/games/123/odds

# Get odds for a week
curl https://your-app.railway.app/api/games/week/1/odds
```

## Database Schema

The `betting_odds` table includes:
- `id`: Primary key
- `game_id`: Foreign key to games table
- `sportsbook`: Name of the sportsbook
- `spread`: Point spread value
- `spread_team`: Team the spread applies to
- `total`: Over/under total
- `home_team_odds`: Home team moneyline odds
- `away_team_odds`: Away team moneyline odds
- `odds_type`: Format of odds (american, decimal, fractional)
- `last_updated`: Timestamp of last update

## Error Handling

The service includes comprehensive error handling:
- **API Key Validation**: Checks if API key is configured
- **Network Errors**: Handles connection issues
- **API Errors**: Processes HTTP error responses
- **Data Validation**: Validates API response data
- **Graceful Degradation**: Continues operation if odds unavailable

## Monitoring

The service provides logging for:
- Successful odds updates
- API errors and network issues
- Scheduled task execution
- Data cleanup operations

## Security Considerations

- API key is stored as environment variable
- No sensitive data exposed in logs
- Rate limiting handled by The Odds API
- Input validation on all endpoints

## Future Enhancements

Potential improvements:
- Support for additional betting markets (moneylines, props)
- Historical odds tracking
- Multiple sportsbook comparison
- Odds change notifications
- Integration with more sportsbooks
