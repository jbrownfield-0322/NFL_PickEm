# Railway Functions for NFL Pick'em Automation

This directory contains Railway Functions that automate the loading of NFL games and updating of scores for your pick'em application.

## Functions Overview

### 1. Game Loader (`game-loader.js`)
- **Purpose**: Automatically loads NFL games into your database
- **Schedule**: Every Sunday at midnight
- **Usage**: Can be triggered manually or automatically

### 2. Score Updater (`score-updater.js`)
- **Purpose**: Updates game scores with predefined results
- **Schedule**: Every Monday at 2 AM
- **Usage**: Updates scores after games finish

### 3. Live Score Updater (`live-score-updater.js`)
- **Purpose**: Fetches live scores from external APIs
- **Schedule**: Every 15 minutes on Sunday and Monday
- **Usage**: Real-time score updates during games

## Setup Instructions

### 1. Deploy to Railway

1. **Create a new Railway project** for functions:
   ```bash
   railway login
   railway init
   ```

2. **Deploy the functions**:
   ```bash
   railway up
   ```

3. **Set environment variables**:
   ```bash
   railway variables set API_BASE=https://your-main-app.railway.app/api
   ```

### 2. Configure Scheduling

The functions are configured to run automatically:

- **Game Loader**: Sundays at midnight (loads next week's games)
- **Score Updater**: Mondays at 2 AM (updates previous day's scores)
- **Live Score Updater**: Every 15 minutes on game days

### 3. Manual Triggering

You can also trigger functions manually:

```bash
# Load games for a specific week
railway functions invoke game-loader --data '{"week": 1}'

# Update scores for a specific date
railway functions invoke score-updater --data '{"date": "2024-09-08"}'

# Update live scores
railway functions invoke live-score-updater
```

## Integration with Live APIs

### Option 1: ESPN API
```javascript
// In live-score-updater.js
const response = await fetch('https://api.espn.com/v1/sports/football/nfl/scoreboard', {
  headers: { 'Authorization': `Bearer ${process.env.ESPN_API_KEY}` }
});
```

### Option 2: SportsData.io API
```javascript
// In live-score-updater.js
const response = await fetch('https://api.sportsdata.io/v3/nfl/scores/json/ScoresByWeek/2024/1', {
  headers: { 'Ocp-Apim-Subscription-Key': process.env.SPORTSDATA_API_KEY }
});
```

### Option 3: The Odds API
```javascript
// In live-score-updater.js
const response = await fetch('https://api.the-odds-api.com/v4/sports/americanfootball_nfl/scores', {
  headers: { 'x-api-key': process.env.ODDS_API_KEY }
});
```

## Environment Variables

Set these in your Railway project:

```bash
# Required
API_BASE=https://your-main-app.railway.app/api

# Optional (for live score APIs)
ESPN_API_KEY=your_espn_api_key
SPORTSDATA_API_KEY=your_sportsdata_api_key
ODDS_API_KEY=your_odds_api_key
```

## Customization

### Adding More Weeks

Edit `game-loader.js` to add more weeks to the `NFL_SCHEDULE_2024` object:

```javascript
const NFL_SCHEDULE_2024 = {
  week1: [ /* games */ ],
  week2: [ /* games */ ],
  week3: [ /* games */ ],
  // ... add more weeks
};
```

### Adding More Results

Edit `score-updater.js` to add more results to the `GAME_RESULTS` object:

```javascript
const GAME_RESULTS = {
  "2024-09-05": [ /* results */ ],
  "2024-09-08": [ /* results */ ],
  "2024-09-12": [ /* results */ ],
  // ... add more dates
};
```

### Custom Scheduling

Modify `railway.json` to change when functions run:

```json
{
  "functions": {
    "game-loader": {
      "schedule": "0 0 * * 0", // Cron expression
      "description": "Loads NFL games"
    }
  }
}
```

## Monitoring

### View Function Logs
```bash
railway logs --function game-loader
railway logs --function score-updater
railway logs --function live-score-updater
```

### Check Function Status
```bash
railway functions list
```

## Troubleshooting

### Common Issues

1. **API Connection Errors**
   - Check that `API_BASE` is correct
   - Verify your main app is running
   - Check network connectivity

2. **Authentication Errors**
   - Verify API keys are set correctly
   - Check API key permissions

3. **Scheduling Issues**
   - Verify cron expressions are correct
   - Check Railway's function scheduling

### Debug Mode

Add logging to functions:

```javascript
console.log('Function started with data:', req.query);
console.log('Processing game:', game);
console.log('Function completed successfully');
```

## Security Considerations

1. **API Keys**: Store sensitive API keys as environment variables
2. **Rate Limiting**: Be mindful of API rate limits
3. **Error Handling**: Implement proper error handling and retries
4. **Validation**: Validate all data before processing

## Cost Optimization

1. **Scheduling**: Only run functions when needed
2. **API Calls**: Minimize external API calls
3. **Caching**: Cache results when possible
4. **Monitoring**: Monitor function execution times

## Support

For issues or questions:
1. Check Railway's documentation
2. Review function logs
3. Test functions manually
4. Contact Railway support if needed
