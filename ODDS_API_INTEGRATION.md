# Betting Odds Integration with The Odds API

This document describes how to set up and use the betting odds integration in the NFL Pick'em application.

## Overview

The application now includes betting odds from The Odds API, displaying spreads, totals, and moneyline odds for each NFL game. This helps users make more informed picks by considering the betting market's perspective.

## Setup

### 1. Get The Odds API Key

1. Visit [The Odds API](https://the-odds-api.com/)
2. Sign up for a free account
3. Get your API key from the dashboard
4. Note: Free tier includes 500 requests per month

### 2. Configure API Key

Add your API key to `src/main/resources/application.properties`:

```properties
# The Odds API Configuration
theodds.api.key=your_actual_api_key_here
theodds.api.base-url=https://api.the-odds-api.com
```

### 3. Build and Run

The application will automatically include the new odds functionality.

## Features

### Backend

- **BettingOdds Model**: Stores spread, total, and moneyline odds
- **OddsService**: Fetches odds from The Odds API
- **New API Endpoints**:
  - `GET /api/games/with-odds` - Games with betting odds
  - `GET /api/games/week/{week}/with-odds` - Week games with odds
  - `POST /api/games/week/{week}/fetch-odds` - Fetch latest odds for a week

### Frontend

- **Toggle Control**: Checkbox to show/hide betting odds
- **Fetch Button**: Manually fetch latest odds for the selected week
- **Odds Display**: Three new columns showing:
  - **Spread**: Point spread with favorite indicator (H/A)
  - **Total**: Over/under total points
  - **Odds**: Moneyline odds for both teams

## How It Works

### 1. Odds Fetching

When you click "Fetch Latest Odds":
1. The backend calls The Odds API for the selected week
2. Odds are processed and stored in the database
3. Games are refreshed to show the new odds

### 2. Data Display

- **Spread**: Shows which team is favored and by how many points
  - Example: "H -3.5" means home team is favored by 3.5 points
  - Example: "A +2.5" means away team is getting 2.5 points
- **Total**: Combined score prediction
  - Example: "45.5" means the over/under is 45.5 points
- **Odds**: American odds format
  - Positive numbers (+150): $100 bet wins $150
  - Negative numbers (-110): $110 bet wins $100

### 3. Team Name Matching

The system matches teams between your games and The Odds API by comparing team names. Ensure team names in your database match the API's format.

## API Rate Limits

- **Free Tier**: 500 requests per month
- **Paid Tiers**: Higher limits available
- **Recommendation**: Fetch odds once per week to stay within limits

## Troubleshooting

### Common Issues

1. **"No odds available"**
   - Check if the game has started (odds may not be available for completed games)
   - Verify your API key is correct
   - Check API rate limits

2. **Team name mismatches**
   - Compare team names in your database with The Odds API
   - Update team names if necessary

3. **API errors**
   - Check network connectivity
   - Verify API key validity
   - Check API status at [The Odds API status page](https://the-odds-api.com/status)

### Debug Mode

Enable debug logging by adding to `application.properties`:

```properties
logging.level.com.nflpickem.pickem.service.OddsService=DEBUG
```

## Future Enhancements

- **Multiple Sportsbooks**: Show odds from different bookmakers
- **Line Movement**: Track how odds change over time
- **Historical Data**: Store odds history for analysis
- **Push Notifications**: Alert users when odds change significantly

## Support

For issues with The Odds API:
- [API Documentation](https://the-odds-api.com/docs/)
- [Support](https://the-odds-api.com/support)

For application issues:
- Check the application logs
- Verify configuration settings
- Ensure database connectivity
