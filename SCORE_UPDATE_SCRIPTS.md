# NFL Pick'em Score Update Scripts

This directory contains PowerShell scripts to manually trigger score updates from The Odds API.

## 📁 Files

- **`update-scores.ps1`** - Full-featured script with error handling and logging
- **`update-scores-simple.ps1`** - Simple version for quick testing
- **`update-scores.bat`** - Batch file wrapper for easy execution
- **`SCORE_UPDATE_SCRIPTS.md`** - This documentation file

## 🚀 Quick Start

### Option 1: Batch File (Easiest)
```cmd
# Use default localhost:8080
update-scores.bat

# Use custom API URL
update-scores.bat https://your-app.railway.app
```

### Option 2: PowerShell Simple Script
```powershell
# Use default localhost:8080
.\update-scores-simple.ps1

# Use custom API URL
.\update-scores-simple.ps1 -ApiUrl "https://your-app.railway.app"
```

### Option 3: PowerShell Full Script
```powershell
# Use default localhost:8080
.\update-scores.ps1

# Use custom API URL
.\update-scores.ps1 -ApiUrl "https://your-app.railway.app"

# Verbose output
.\update-scores.ps1 -Verbose
```

## 🔧 Configuration

### Default Settings
- **API URL**: `http://localhost:8080`
- **Endpoint**: `/api/games/update-scores`
- **Timeout**: 30 seconds

### Environment Variables Required
Make sure your application has these configured:
- `ODDS_API_KEY` - Your The Odds API key
- `ODDS_API_BASE_URL` - The Odds API base URL

## 📊 What the Script Does

1. **Tests API Connectivity** - Checks if the server is reachable
2. **Calls Score Update Endpoint** - Triggers the scoring service
3. **Fetches Live Scores** - Gets real NFL game results from The Odds API
4. **Updates Database** - Marks games as scored and updates pick results
5. **Reports Results** - Shows success/failure with detailed logging

## 🎯 Use Cases

### Development & Testing
```powershell
# Test with local development server
.\update-scores-simple.ps1 -ApiUrl "http://localhost:8080"
```

### Production Updates
```powershell
# Update scores on production server
.\update-scores.ps1 -ApiUrl "https://your-app.railway.app" -Verbose
```

### Troubleshooting
```powershell
# Get detailed output for debugging
.\update-scores.ps1 -Verbose
```

## 🔍 Output Examples

### Success
```
🏈 NFL Pick'em Score Update Script
=====================================

🔍 Testing API connectivity...
✅ API is reachable

🚀 Starting score update process...
📍 API URL: http://localhost:8080/api/games/update-scores
⏰ Timeout: 30 seconds

📡 Calling API endpoint...
✅ SUCCESS!
📊 Response: Scores updated successfully from API
⏱️  Duration: 2.34 seconds

=====================================
🎉 Score update completed successfully!
📊 Check your leaderboard to see updated scores.
```

### Error
```
❌ HTTP ERROR!
🔍 Status Code: 500
📝 Error Message: The remote server returned an error: (500) Internal Server Error.
📄 Response Body: Error updating scores: ODDS_API_KEY is not configured
⏱️  Duration: 1.23 seconds

💥 Score update failed!
🔧 Troubleshooting tips:
  • Check if the API server is running
  • Verify the API URL is correct
  • Check if ODDS_API_KEY is configured
  • Review server logs for detailed error information
```

## 🛠️ Troubleshooting

### Common Issues

1. **"Execution Policy" Error**
   ```powershell
   Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
   ```

2. **"API not reachable"**
   - Check if the server is running
   - Verify the API URL is correct
   - Check firewall/network settings

3. **"ODDS_API_KEY not configured"**
   - Set the environment variable in your application
   - Restart the application after setting the variable

4. **"Timeout" Error**
   - The API call is taking too long
   - Check server performance
   - Verify The Odds API is responding

### Manual Testing
You can also test the endpoint directly with curl:
```bash
curl -X POST http://localhost:8080/api/games/update-scores
```

## 📝 Notes

- The script will only update games that haven't been scored yet
- Games are matched using fuzzy team name matching
- The system falls back to legacy scoring if The Odds API is unavailable
- Score updates are logged in the application console
- The endpoint returns a success message or error details

## 🔄 Automation

For automated score updates, the application already has a scheduled task that runs every hour on game days. These scripts are primarily for:
- Manual testing during development
- Emergency score updates
- Troubleshooting scoring issues
- Immediate updates when needed
