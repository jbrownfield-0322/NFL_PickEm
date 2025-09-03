const fs = require('fs');

// Configuration
const API_BASE = process.env.API_BASE || 'https://nflpickem-42ad9a71cf70.herokuapp.com/api';
const ODDS_API_KEY = process.env.THEODDS_API_KEY || '04f5a0643a9fe4b5b3b390c8037ae885';
const ODDS_API_BASE = 'https://api.the-odds-api.com';

async function updateGamesWithOdds() {
  try {
    console.log('🏈 Updating existing NFL games with betting odds...');
    console.log(`Using API Key: ${ODDS_API_KEY.substring(0, 8)}...`);
    
    // Step 1: Get existing games from our database
    console.log('\n1️⃣ Fetching existing games from database...');
    const gamesResponse = await fetch(`${API_BASE}/games`);
    
    if (!gamesResponse.ok) {
      throw new Error(`Failed to fetch games: ${gamesResponse.status} ${gamesResponse.statusText}`);
    }
    
    const existingGames = await gamesResponse.json();
    console.log(`✅ Found ${existingGames.length} existing games in database`);
    
    if (existingGames.length === 0) {
      console.log('❌ No games found in database. Please add games first.');
      return;
    }
    
    // Step 2: Get betting odds from The Odds API
    console.log('\n2️⃣ Fetching betting odds from The Odds API...');
    const oddsResponse = await fetch(
      `${ODDS_API_BASE}/v4/sports/americanfootball_nfl/odds?apiKey=${ODDS_API_KEY}&regions=us&markets=spreads,totals&oddsFormat=american`
    );
    
    if (!oddsResponse.ok) {
      const errorText = await oddsResponse.text();
      throw new Error(`Failed to fetch odds: ${oddsResponse.status} ${oddsResponse.statusText} - ${errorText}`);
    }
    
    const oddsData = await oddsResponse.json();
    console.log(`✅ Found ${oddsData.length} games with odds from The Odds API`);
    
    // Step 3: Match games and update with odds
    console.log('\n3️⃣ Matching games and updating with odds...');
    await updateGamesWithOddsData(existingGames, oddsData);
    
  } catch (error) {
    console.error('❌ Error updating games with odds:', error.message);
  }
}

async function updateGamesWithOddsData(existingGames, oddsData) {
  let updatedCount = 0;
  let errorCount = 0;
  let noMatchCount = 0;
  
  for (const existingGame of existingGames) {
    try {
      // Find matching game in odds data
      const matchingOdds = findMatchingGame(existingGame, oddsData);
      
      if (!matchingOdds) {
        console.log(`⚠️  No odds found for: ${existingGame.awayTeam} @ ${existingGame.homeTeam} (Week ${existingGame.week})`);
        noMatchCount++;
        continue;
      }
      
      // Extract betting odds
      const bettingOdds = extractBettingOdds(matchingOdds);
      
      if (!bettingOdds) {
        console.log(`⚠️  No betting markets found for: ${existingGame.awayTeam} @ ${existingGame.homeTeam}`);
        noMatchCount++;
        continue;
      }
      
      // Update game with odds
      const updateResponse = await fetch(`${API_BASE}/games/${existingGame.id}/odds`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(bettingOdds),
      });
      
      if (updateResponse.ok) {
        console.log(`✅ Updated odds for: ${existingGame.awayTeam} @ ${existingGame.homeTeam} (Week ${existingGame.week})`);
        console.log(`   Spread: ${bettingOdds.spreadLine} (${bettingOdds.spreadOdds})`);
        console.log(`   Total: ${bettingOdds.totalLine} (${bettingOdds.totalOdds})`);
        updatedCount++;
      } else {
        const errorText = await updateResponse.text();
        console.error(`❌ Failed to update odds for: ${existingGame.awayTeam} @ ${existingGame.homeTeam} - Status: ${updateResponse.status} - ${errorText}`);
        errorCount++;
      }
      
      // Add a small delay to avoid overwhelming the API
      await new Promise(resolve => setTimeout(resolve, 100));
      
    } catch (error) {
      console.error(`❌ Error updating game: ${existingGame.awayTeam} @ ${existingGame.homeTeam} - ${error.message}`);
      errorCount++;
    }
  }
  
  console.log('\n=== UPDATE COMPLETE ===');
  console.log(`✅ Successfully updated: ${updatedCount} games with odds`);
  console.log(`❌ Failed to update: ${errorCount} games`);
  console.log(`⚠️  No odds found for: ${noMatchCount} games`);
  console.log(`📊 Total games processed: ${existingGames.length}`);
}

function findMatchingGame(existingGame, oddsData) {
  // Try to find exact match by team names
  return oddsData.find(oddsGame => {
    const awayMatch = oddsGame.away_team === existingGame.awayTeam;
    const homeMatch = oddsGame.home_team === existingGame.homeTeam;
    return awayMatch && homeMatch;
  });
}

function extractBettingOdds(oddsGame) {
  try {
    const spreadMarket = oddsGame.bookmakers?.[0]?.markets?.find(m => m.key === 'spreads');
    const totalMarket = oddsGame.bookmakers?.[0]?.markets?.find(m => m.key === 'totals');
    
    if (!spreadMarket || !totalMarket) {
      return null;
    }
    
    const spreadOutcome = spreadMarket.outcomes?.[0];
    const totalOutcome = totalMarket.outcomes?.[0];
    
    if (!spreadOutcome || !totalOutcome) {
      return null;
    }
    
    return {
      spreadLine: spreadOutcome.point,
      spreadOdds: spreadOutcome.price,
      totalLine: totalOutcome.point,
      totalOdds: totalOutcome.price,
      lastUpdate: oddsGame.last_update,
      bookmaker: oddsGame.bookmakers?.[0]?.title || 'Unknown'
    };
  } catch (error) {
    console.error('Error extracting betting odds:', error);
    return null;
  }
}

// Run the script
updateGamesWithOdds();
