const fs = require('fs');
const https = require('https');
const { JSDOM } = require('jsdom');

// Configuration
const API_BASE = process.env.API_BASE || 'http://localhost:8080/api';
const SEASON = process.env.SEASON || '2024';

// Helper function to make HTTPS requests
function makeRequest(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', (chunk) => data += chunk);
      res.on('end', () => resolve(data));
    }).on('error', reject);
  });
}

// Get games from database
async function getGamesFromDatabase() {
  try {
    const response = await fetch(`${API_BASE}/games`);
    if (!response.ok) {
      throw new Error(`Failed to fetch games: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error fetching games from database:', error);
    return [];
  }
}

// Update game score in database
async function updateGameScore(gameId, winningTeam) {
  try {
    const response = await fetch(`${API_BASE}/games/${gameId}/score`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ winningTeam }),
    });
    
    if (response.ok) {
      const updatedGame = await response.json();
      console.log(`✅ Updated score for game ${gameId}: ${winningTeam} wins`);
      return updatedGame;
    } else {
      console.error(`❌ Failed to update game ${gameId}: ${response.status}`);
      return null;
    }
  } catch (error) {
    console.error(`❌ Error updating game ${gameId}:`, error);
    return null;
  }
}

// Scrape NFL scores from Pro-Football-Reference
async function scrapeNFLScores() {
  console.log(`Scraping NFL scores for ${SEASON} season...`);
  
  const url = `https://www.pro-football-reference.com/years/${SEASON}/games.htm`;
  
  try {
    const html = await makeRequest(url);
    const dom = new JSDOM(html);
    const document = dom.window.document;
    
    // Find the schedule table
    const table = document.querySelector('#games');
    if (!table) {
      throw new Error('Could not find games table');
    }
    
    const rows = table.querySelectorAll('tbody tr');
    console.log(`Found ${rows.length} game rows`);
    
    const gameResults = [];
    
    for (const row of rows) {
      const cells = row.querySelectorAll('td');
      if (cells.length < 12) continue;
      
      // Skip header rows or empty rows
      if (row.classList.contains('thead') || cells[0].textContent.trim() === '') {
        continue;
      }
      
      try {
        const week = parseInt(cells[0].textContent.trim());
        const awayTeam = cells[3].textContent.trim();
        const homeTeam = cells[5].textContent.trim();
        const awayScore = cells[4].textContent.trim();
        const homeScore = cells[6].textContent.trim();
        
        // Skip if we don't have valid data or if game hasn't been played
        if (!week || !awayTeam || !homeTeam || awayScore === '' || homeScore === '') {
          continue;
        }
        
        // Parse scores
        const awayScoreNum = parseInt(awayScore);
        const homeScoreNum = parseInt(homeScore);
        
        if (isNaN(awayScoreNum) || isNaN(homeScoreNum)) {
          continue;
        }
        
        // Determine winner
        let winningTeam;
        if (awayScoreNum > homeScoreNum) {
          winningTeam = awayTeam;
        } else if (homeScoreNum > awayScoreNum) {
          winningTeam = homeTeam;
        } else {
          winningTeam = "TIE";
        }
        
        const gameResult = {
          week: week,
          awayTeam: awayTeam,
          homeTeam: homeTeam,
          awayScore: awayScoreNum,
          homeScore: homeScoreNum,
          winningTeam: winningTeam
        };
        
        gameResults.push(gameResult);
        console.log(`Parsed result: Week ${week} - ${awayTeam} ${awayScore} @ ${homeTeam} ${homeScore} (${winningTeam} wins)`);
        
      } catch (error) {
        console.error('Error parsing row:', error);
        continue;
      }
    }
    
    console.log(`Successfully parsed ${gameResults.length} game results`);
    return gameResults;
    
  } catch (error) {
    console.error('Error scraping NFL scores:', error);
    throw error;
  }
}

// Update scores in database
async function updateScoresInDatabase(gameResults) {
  console.log(`Updating ${gameResults.length} game scores in database...`);
  
  // Get current games from database
  const dbGames = await getGamesFromDatabase();
  console.log(`Found ${dbGames.length} games in database`);
  
  let updatedCount = 0;
  let skippedCount = 0;
  let errorCount = 0;
  
  for (const result of gameResults) {
    try {
      // Find matching game in database
      const dbGame = dbGames.find(g => 
        g.week === result.week && 
        g.awayTeam === result.awayTeam && 
        g.homeTeam === result.homeTeam
      );
      
      if (!dbGame) {
        console.warn(`Game not found in database: ${result.awayTeam} @ ${result.homeTeam} (Week ${result.week})`);
        errorCount++;
        continue;
      }
      
      // Skip if already scored
      if (dbGame.scored) {
        console.log(`Game already scored: ${result.awayTeam} @ ${result.homeTeam} (Week ${result.week})`);
        skippedCount++;
        continue;
      }
      
      // Update the score
      const updated = await updateGameScore(dbGame.id, result.winningTeam);
      if (updated) {
        updatedCount++;
      } else {
        errorCount++;
      }
      
      // Add a small delay to avoid overwhelming the API
      await new Promise(resolve => setTimeout(resolve, 100));
      
    } catch (error) {
      console.error(`Error updating score for ${result.awayTeam} @ ${result.homeTeam}:`, error);
      errorCount++;
    }
  }
  
  console.log(`\nUpdate Summary:`);
  console.log(`✅ Successfully updated: ${updatedCount} games`);
  console.log(`⏭️  Skipped (already scored): ${skippedCount} games`);
  console.log(`❌ Failed to update: ${errorCount} games`);
  
  return { updatedCount, skippedCount, errorCount };
}

// Save results to JSON file
function saveResultsToFile(gameResults) {
  const filename = `nfl-scores-${SEASON}-${new Date().toISOString().split('T')[0]}.json`;
  try {
    fs.writeFileSync(filename, JSON.stringify(gameResults, null, 2));
    console.log(`✅ Saved ${gameResults.length} game results to ${filename}`);
  } catch (error) {
    console.error('Error saving results to file:', error);
  }
}

// Main function
async function main() {
  try {
    console.log('🏈 NFL Score Scraper');
    console.log('====================');
    
    // Scrape the scores
    const gameResults = await scrapeNFLScores();
    
    // Save to file for reference
    saveResultsToFile(gameResults);
    
    // Update scores in database
    if (gameResults.length > 0) {
      await updateScoresInDatabase(gameResults);
    } else {
      console.log('No game results to update');
    }
    
    console.log('\n🎉 NFL Score updating complete!');
    
  } catch (error) {
    console.error('❌ Error in main function:', error);
    process.exit(1);
  }
}

// Run the script
if (require.main === module) {
  main();
}

module.exports = { scrapeNFLScores, updateScoresInDatabase };
