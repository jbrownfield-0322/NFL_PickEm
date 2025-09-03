const fs = require('fs');

// Configuration
const API_BASE = process.env.API_BASE || 'https://nflpickem-42ad9a71cf70.herokuapp.com/api';
const GAMES_FILE = 'scripts/nfl-schedule-2025.json';
const PRESEASON_WEEKS = [1, 2, 3]; // Weeks to skip (preseason)

async function add2025Games() {
  try {
    // Read the 2025 NFL schedule file
    const gamesData = fs.readFileSync(GAMES_FILE, 'utf8');
    const allGames = JSON.parse(gamesData);
    
    // Get unique weeks in the data
    const uniqueWeeks = [...new Set(allGames.map(game => game.week))].sort((a, b) => a - b);
    
    console.log(`Found ${allGames.length} total games in 2025 schedule`);
    console.log(`Weeks found in data: ${uniqueWeeks.join(', ')}`);
    
    // Filter out preseason games
    const regularSeasonGames = allGames.filter(game => !PRESEASON_WEEKS.includes(game.week));
    
    if (regularSeasonGames.length === 0) {
      console.log('\n⚠️  WARNING: All games appear to be preseason games!');
      console.log('This could mean:');
      console.log('1. The 2025 regular season schedule hasn\'t been released yet');
      console.log('2. The scraper only captured preseason data');
      console.log('3. The data source needs to be updated');
      console.log('\nPreseason games are typically not used for pick\'em pools.');
      
      const usePreseason = process.argv.includes('--include-preseason');
      if (usePreseason) {
        console.log('\n🔄 Loading preseason games anyway (--include-preseason flag detected)...');
        await loadGames(allGames, 'preseason');
      } else {
        console.log('\n🚫 Skipping all games. To load preseason games, run with: node add-sample-games.js --include-preseason');
        return;
      }
    } else {
      console.log(`Filtering out ${allGames.length - regularSeasonGames.length} preseason games (Weeks ${PRESEASON_WEEKS.join(', ')})`);
      console.log(`Loading ${regularSeasonGames.length} regular season and playoff games...`);
      await loadGames(regularSeasonGames, 'regular season');
    }
    
  } catch (error) {
    console.error('Error reading 2025 NFL schedule file:', error.message);
  }
}

async function loadGames(games, gameType) {
  let successCount = 0;
  let errorCount = 0;
  
  // Add each game
  for (const game of games) {
    try {
      const response = await fetch(`${API_BASE}/games`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(game),
      });
      
      if (response.ok) {
        const savedGame = await response.json();
        console.log(`✅ Added: ${game.awayTeam} @ ${game.homeTeam} (Week ${game.week})`);
        successCount++;
      } else {
        const errorText = await response.text();
        console.error(`❌ Failed to add: ${game.awayTeam} @ ${game.homeTeam} - Status: ${response.status} - ${errorText}`);
        errorCount++;
      }
      
      // Add a small delay to avoid overwhelming the API
      await new Promise(resolve => setTimeout(resolve, 100));
      
    } catch (error) {
      console.error(`❌ Error adding game: ${game.awayTeam} @ ${game.homeTeam} - ${error.message}`);
      errorCount++;
    }
  }
  
  console.log('\n=== LOADING COMPLETE ===');
  console.log(`✅ Successfully added: ${successCount} ${gameType} games`);
  console.log(`❌ Failed to add: ${errorCount} games`);
  console.log(`📊 Total games processed: ${games.length}`);
}

// Run the script
add2025Games();
