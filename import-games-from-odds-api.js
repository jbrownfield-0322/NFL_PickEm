const fs = require('fs');

// Configuration
const API_BASE = process.env.API_BASE || 'https://nflpickem-42ad9a71cf70.herokuapp.com/api';
const ODDS_API_KEY = process.env.THEODDS_API_KEY || '04f5a0643a9fe4b5b3b390c8037ae885';
const ODDS_API_BASE = 'https://api.the-odds-api.com';

async function importGamesFromOddsAPI() {
  try {
    console.log('🏈 Importing NFL games from The Odds API...');
    console.log(`Using API Key: ${ODDS_API_KEY.substring(0, 8)}...`);
    
    // Step 1: Get available sports to confirm NFL is available
    console.log('\n1️⃣ Checking available sports...');
    const sportsResponse = await fetch(`${ODDS_API_BASE}/v4/sports?apiKey=${ODDS_API_KEY}`);
    
    if (!sportsResponse.ok) {
      throw new Error(`Failed to fetch sports: ${sportsResponse.status} ${sportsResponse.statusText}`);
    }
    
    const sports = await sportsResponse.json();
    const nflSport = sports.find(sport => sport.key === 'americanfootball_nfl');
    
    if (!nflSport) {
      throw new Error('NFL not found in available sports');
    }
    
    console.log(`✅ Found NFL: ${nflSport.title} (${nflSport.description})`);
    
    // Step 2: Get NFL games with odds (this includes the schedule)
    console.log('\n2️⃣ Fetching NFL games from The Odds API...');
    const gamesResponse = await fetch(
      `${ODDS_API_BASE}/v4/sports/americanfootball_nfl/odds?apiKey=${ODDS_API_KEY}&regions=us&markets=spreads,totals&oddsFormat=american`
    );
    
    if (!gamesResponse.ok) {
      const errorText = await gamesResponse.text();
      throw new Error(`Failed to fetch NFL games: ${gamesResponse.status} ${gamesResponse.statusText} - ${errorText}`);
    }
    
    const oddsData = await gamesResponse.json();
    console.log(`✅ Found ${oddsData.length} games from The Odds API`);
    
    // Step 3: Transform odds data to our game format
    console.log('\n3️⃣ Transforming data to our game format...');
    const games = oddsData.map(oddsGame => {
      // Extract game info from odds data
      const game = {
        week: determineWeek(oddsGame.commence_time),
        awayTeam: oddsGame.away_team,
        homeTeam: oddsGame.home_team,
        kickoffTime: oddsGame.commence_time,
        winningTeam: "", // Games haven't been played yet
        scored: false
      };
      
      return game;
    });
    
    // Step 4: Filter out preseason games (typically early August)
    const regularSeasonGames = games.filter(game => {
      const gameDate = new Date(game.kickoffTime);
      const preseasonStart = new Date('2025-08-01');
      return gameDate >= preseasonStart;
    });
    
    console.log(`📅 Regular season games (after Aug 1): ${regularSeasonGames.length}`);
    console.log(`📅 Preseason games filtered out: ${games.length - regularSeasonGames.length}`);
    
    if (regularSeasonGames.length === 0) {
      console.log('\n⚠️  No regular season games found. This might mean:');
      console.log('1. The 2025 regular season schedule hasn\'t been released yet');
      console.log('2. The Odds API only has preseason data currently');
      console.log('3. The date filtering needs adjustment');
      return;
    }
    
    // Step 5: Import games to our database
    console.log('\n4️⃣ Importing games to database...');
    await importGamesToDatabase(regularSeasonGames);
    
  } catch (error) {
    console.error('❌ Error importing games:', error.message);
  }
}

function determineWeek(kickoffTime) {
  // Simple week calculation based on date
  // NFL regular season typically starts first week of September
  const gameDate = new Date(kickoffTime);
  const seasonStart = new Date('2025-09-01');
  
  if (gameDate < seasonStart) {
    return 0; // Preseason
  }
  
  const weeksSinceStart = Math.floor((gameDate - seasonStart) / (7 * 24 * 60 * 60 * 1000));
  return Math.max(1, weeksSinceStart + 1);
}

async function importGamesToDatabase(games) {
  let successCount = 0;
  let errorCount = 0;
  
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
  
  console.log('\n=== IMPORT COMPLETE ===');
  console.log(`✅ Successfully imported: ${successCount} games`);
  console.log(`❌ Failed to import: ${errorCount} games`);
  console.log(`📊 Total games processed: ${games.length}`);
}

// Run the script
importGamesFromOddsAPI();
