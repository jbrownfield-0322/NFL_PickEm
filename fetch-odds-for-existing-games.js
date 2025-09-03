// Script to fetch betting odds for existing games using the backend's OddsService
const API_BASE = process.env.API_BASE || 'https://nflpickem-42ad9a71cf70.herokuapp.com/api';

async function fetchOddsForExistingGames() {
  try {
    console.log('🏈 Fetching betting odds for existing NFL games...');
    
    // Step 1: Get all games from database to see what weeks we have
    console.log('\n1️⃣ Checking existing games in database...');
    const gamesResponse = await fetch(`${API_BASE}/games`);
    
    if (!gamesResponse.ok) {
      throw new Error(`Failed to fetch games: ${gamesResponse.status} ${gamesResponse.statusText}`);
    }
    
    const allGames = await gamesResponse.json();
    console.log(`📊 Found ${allGames.length} games in database`);
    
    if (allGames.length === 0) {
      console.log('❌ No games found in database. Please add games first.');
      return;
    }
    
    // Step 2: Determine which weeks have games
    const weeks = [...new Set(allGames.map(game => game.week))].sort((a, b) => a - b);
    console.log(`📅 Games found for weeks: ${weeks.join(', ')}`);
    
    // Step 3: Use the backend's existing odds fetching system for each week
    console.log('\n2️⃣ Fetching odds for each week using backend OddsService...');
    let successCount = 0;
    let errorCount = 0;
    
    for (const week of weeks) {
      try {
        console.log(`\n🔄 Fetching odds for Week ${week}...`);
        
        // Use the existing backend endpoint that triggers OddsService.fetchOddsForWeek()
        const oddsResponse = await fetch(`${API_BASE}/games/week/${week}/fetch-odds`, {
          method: 'POST',
        });
        
        if (oddsResponse.ok) {
          const result = await oddsResponse.text();
          console.log(`✅ Week ${week}: ${result}`);
          successCount++;
        } else {
          const errorText = await oddsResponse.text();
          console.error(`❌ Week ${week} failed: ${oddsResponse.status} - ${errorText}`);
          errorCount++;
        }
        
        // Add delay between weeks to respect API rate limits
        if (week !== weeks[weeks.length - 1]) { // Not the last week
          console.log(`⏳ Waiting 3 seconds before next week...`);
          await new Promise(resolve => setTimeout(resolve, 3000));
        }
        
      } catch (error) {
        console.error(`❌ Error fetching odds for Week ${week}: ${error.message}`);
        errorCount++;
      }
    }
    
    console.log('\n=== ODDS FETCHING COMPLETE ===');
    console.log(`✅ Successfully fetched odds for: ${successCount} weeks`);
    console.log(`❌ Failed to fetch odds for: ${errorCount} weeks`);
    console.log(`📊 Total weeks processed: ${weeks.length}`);
    
    // Step 4: Verify that odds were fetched by checking a few games
    console.log('\n3️⃣ Verifying odds were fetched...');
    await verifyOddsWereFetched();
    
  } catch (error) {
    console.error('❌ Error during odds fetching:', error.message);
  }
}

async function verifyOddsWereFetched() {
  try {
    // Check games with odds endpoint
    const oddsResponse = await fetch(`${API_BASE}/games/with-odds`);
    
    if (oddsResponse.ok) {
      const gamesWithOdds = await oddsResponse.json();
      const gamesWithOddsCount = gamesWithOdds.filter(game => game.odds).length;
      
      console.log(`📊 Games with odds: ${gamesWithOddsCount} out of ${gamesWithOdds.length}`);
      
      if (gamesWithOddsCount > 0) {
        console.log('🎯 Success! Some games now have betting odds');
        
        // Show a sample of games with odds
        const sampleGame = gamesWithOdds.find(game => game.odds);
        if (sampleGame) {
          console.log('\n📋 Sample game with odds:');
          console.log(`   ${sampleGame.awayTeam} @ ${sampleGame.homeTeam} (Week ${sampleGame.week})`);
          console.log(`   Spread: ${sampleGame.odds.spread} (${sampleGame.odds.spreadTeam})`);
          console.log(`   Total: ${sampleGame.odds.total}`);
          console.log(`   Home Odds: ${sampleGame.odds.homeTeamOdds}`);
          console.log(`   Away Odds: ${sampleGame.odds.awayTeamOdds}`);
        }
      } else {
        console.log('⚠️  No games have odds yet. This might mean:');
        console.log('1. The Odds API is still blocking requests from Heroku');
        console.log('2. The odds fetching process needs more time');
        console.log('3. There was an error in the odds processing');
      }
    } else {
      console.error(`❌ Failed to verify odds: ${oddsResponse.status}`);
    }
    
  } catch (error) {
    console.error('❌ Error verifying odds:', error.message);
  }
}

// Run the script
fetchOddsForExistingGames();
