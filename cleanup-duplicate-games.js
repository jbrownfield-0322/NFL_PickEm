// Script to clean up duplicate game records
const API_BASE = process.env.API_BASE || 'https://nflpickem-42ad9a71cf70.herokuapp.com/api';

async function cleanupDuplicateGames() {
  try {
    console.log('🧹 Cleaning up duplicate game records...');
    
    // Step 1: Get all games from database
    console.log('\n1️⃣ Fetching all games from database...');
    const gamesResponse = await fetch(`${API_BASE}/games`);
    
    if (!gamesResponse.ok) {
      throw new Error(`Failed to fetch games: ${gamesResponse.status} ${gamesResponse.statusText}`);
    }
    
    const allGames = await gamesResponse.json();
    console.log(`📊 Found ${allGames.length} total games in database`);
    
    if (allGames.length === 0) {
      console.log('✅ No games to clean up');
      return;
    }
    
    // Step 2: Group games by week and team combination to find duplicates
    const gameGroups = {};
    const duplicates = [];
    
    allGames.forEach(game => {
      const key = `${game.week}-${game.awayTeam}-${game.homeTeam}`;
      if (!gameGroups[key]) {
        gameGroups[key] = [];
      }
      gameGroups[key].push(game);
    });
    
    // Step 3: Identify duplicates (keep the first one, mark others for deletion)
    Object.values(gameGroups).forEach(games => {
      if (games.length > 1) {
        // Keep the first game, mark the rest as duplicates
        duplicates.push(...games.slice(1));
      }
    });
    
    console.log(`🔍 Found ${duplicates.length} duplicate games to remove`);
    
    if (duplicates.length === 0) {
      console.log('✅ No duplicates found');
      return;
    }
    
    // Step 4: Delete duplicate games
    console.log('\n2️⃣ Removing duplicate games...');
    let deletedCount = 0;
    let errorCount = 0;
    
    for (const duplicate of duplicates) {
      try {
        const deleteResponse = await fetch(`${API_BASE}/games/${duplicate.id}`, {
          method: 'DELETE',
        });
        
        if (deleteResponse.ok) {
          console.log(`✅ Deleted duplicate: ${duplicate.awayTeam} @ ${duplicate.homeTeam} (Week ${duplicate.week}) - ID: ${duplicate.id}`);
          deletedCount++;
        } else {
          console.error(`❌ Failed to delete game ${duplicate.id}: ${deleteResponse.status}`);
          errorCount++;
        }
        
        // Small delay to avoid overwhelming the API
        await new Promise(resolve => setTimeout(resolve, 100));
        
      } catch (error) {
        console.error(`❌ Error deleting game ${duplicate.id}: ${error.message}`);
        errorCount++;
      }
    }
    
    console.log('\n=== CLEANUP COMPLETE ===');
    console.log(`✅ Successfully deleted: ${deletedCount} duplicate games`);
    console.log(`❌ Failed to delete: ${errorCount} games`);
    console.log(`📊 Total duplicates processed: ${duplicates.length}`);
    
    // Step 5: Verify final count
    console.log('\n3️⃣ Verifying final game count...');
    const finalResponse = await fetch(`${API_BASE}/games`);
    if (finalResponse.ok) {
      const finalGames = await finalResponse.json();
      console.log(`📊 Final game count: ${finalGames.length} games`);
      
      if (finalGames.length === 272) {
        console.log('🎯 Perfect! You now have exactly 272 unique NFL games (no duplicates)');
      } else {
        console.log(`⚠️  Expected 272 games, but found ${finalGames.length}`);
      }
    }
    
  } catch (error) {
    console.error('❌ Error during cleanup:', error.message);
  }
}

// Run the cleanup
cleanupDuplicateGames();
