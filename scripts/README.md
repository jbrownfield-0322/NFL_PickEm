# NFL Pick'em Scripts

This directory contains scripts for scraping NFL schedules and scores, and loading them into your pick'em application database.

## Scripts Overview

### 1. Schedule Scraper (`scrape-nfl-schedule.js`)
- **Purpose**: Scrapes the complete NFL schedule from Pro-Football-Reference.com
- **Output**: Loads all games into your database via API
- **Usage**: Run once at the beginning of the season

### 2. Score Scraper (`scrape-nfl-scores.js`)
- **Purpose**: Scrapes live NFL scores and updates your database
- **Output**: Updates game results in your database
- **Usage**: Run after games finish to update scores

### 3. Sample Games Loader (`../add-sample-games.js`)
- **Purpose**: Loads predefined sample games for testing
- **Output**: Adds Week 1 sample games to your database
- **Usage**: For testing and development

## Setup

### 1. Install Dependencies

```bash
cd scripts
npm install
```

### 2. Configure Environment Variables

Set these environment variables:

```bash
# Required - Your API base URL
export API_BASE=https://your-app.railway.app/api

# Optional - Season year (defaults to 2024)
export SEASON=2024
```

## Usage

### Load Complete NFL Schedule

```bash
# Scrape and load the entire season schedule
npm run scrape

# Or run directly
node scrape-nfl-schedule.js
```

This will:
1. Scrape the NFL schedule from Pro-Football-Reference.com
2. Save the schedule to `nfl-schedule-2024.json`
3. Load all games into your database via API

### Load Schedule from File

If you've already scraped the schedule and want to reload it:

```bash
npm run scrape:from-file
# Or
node scrape-nfl-schedule.js --from-file
```

### Update Game Scores

```bash
# Scrape and update all game scores
node scrape-nfl-scores.js
```

This will:
1. Scrape current scores from Pro-Football-Reference.com
2. Save results to a dated JSON file
3. Update unscored games in your database

### Load Sample Games (Testing)

```bash
npm run load-sample
# Or
node ../add-sample-games.js
```

## Output Files

### Schedule Files
- `nfl-schedule-2024.json` - Complete season schedule
- `nfl-scores-2024-YYYY-MM-DD.json` - Daily score results

### Example Schedule Format
```json
[
  {
    "week": 1,
    "awayTeam": "Green Bay Packers",
    "homeTeam": "Philadelphia Eagles",
    "kickoffTime": "2024-09-05T20:20:00Z",
    "winningTeam": "",
    "scored": false
  }
]
```

### Example Score Format
```json
[
  {
    "week": 1,
    "awayTeam": "Green Bay Packers",
    "homeTeam": "Philadelphia Eagles",
    "awayScore": 24,
    "homeScore": 31,
    "winningTeam": "Philadelphia Eagles"
  }
]
```

## Scheduling

### For Production Use

You can set up these scripts to run automatically:

#### 1. Beginning of Season (Once)
```bash
# Run this once at the start of the season
node scrape-nfl-schedule.js
```

#### 2. After Games (Weekly)
```bash
# Run this after games finish (e.g., Monday mornings)
node scrape-nfl-scores.js
```

#### 3. Using Cron (Linux/Mac)
```bash
# Add to crontab
# Load schedule once at season start
0 0 1 9 * cd /path/to/scripts && node scrape-nfl-schedule.js

# Update scores every Monday at 2 AM
0 2 * * 1 cd /path/to/scripts && node scrape-nfl-scores.js
```

#### 4. Using Railway Functions
You can also use the Railway Functions we created earlier for automated scheduling.

## Error Handling

### Common Issues

1. **Network Errors**
   - Check internet connection
   - Verify Pro-Football-Reference.com is accessible
   - Check if the site structure has changed

2. **API Errors**
   - Verify `API_BASE` is correct
   - Check if your app is running
   - Ensure API endpoints are working

3. **Parsing Errors**
   - Check if team names match your database
   - Verify date/time formats
   - Look for changes in website structure

### Debug Mode

Add logging to see what's happening:

```bash
# Enable debug logging
DEBUG=* node scrape-nfl-schedule.js
```

## Team Name Mapping

The scripts include mappings for all 32 NFL teams. If team names don't match, update the `TEAM_MAPPINGS` object in the script.

## Time Zone Handling

- All kickoff times are converted to UTC
- Assumes Eastern Time for NFL games
- Adjusts for daylight saving time automatically

## Rate Limiting

The scripts include built-in delays to avoid overwhelming APIs:
- 100ms delay between API calls
- Respects website rate limits
- Handles network timeouts gracefully

## Backup and Recovery

### Backup Schedule
```bash
# Save current schedule
cp nfl-schedule-2024.json nfl-schedule-2024-backup.json
```

### Restore from Backup
```bash
# Restore from backup
cp nfl-schedule-2024-backup.json nfl-schedule-2024.json
node scrape-nfl-schedule.js --from-file
```

## Monitoring

### Check Script Status
```bash
# View recent logs
tail -f scrape.log

# Check database status
curl ${API_BASE}/games | jq '. | length'
```

### Verify Data
```bash
# Check games in database
curl ${API_BASE}/games | jq '.[] | {week, awayTeam, homeTeam, scored}'

# Check specific week
curl ${API_BASE}/games/week/1 | jq '.'
```

## Troubleshooting

### Script Won't Run
1. Check Node.js version: `node --version`
2. Install dependencies: `npm install`
3. Check environment variables: `echo $API_BASE`

### No Games Found
1. Verify the season year is correct
2. Check if Pro-Football-Reference.com structure changed
3. Look for parsing errors in logs

### API Errors
1. Test API manually: `curl ${API_BASE}/games`
2. Check authentication if required
3. Verify API endpoints are working

### Score Updates Not Working
1. Check if games are already scored
2. Verify team name matching
3. Look for parsing errors in score data

## Support

For issues:
1. Check the logs for error messages
2. Verify environment variables
3. Test API endpoints manually
4. Check Pro-Football-Reference.com for changes

## Legal Notice

These scripts scrape publicly available data from Pro-Football-Reference.com. Please respect their terms of service and rate limits. Consider using official APIs for production applications.
