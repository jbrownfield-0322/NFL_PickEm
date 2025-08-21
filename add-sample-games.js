const fs = require('fs');

// Configuration
const API_BASE = process.env.API_BASE || 'http://localhost:8080/api';
const SAMPLE_GAMES_FILE = 'sample-games-2024.json';

async function addSampleGames() {
  try {
    // Read the sample games file
    const gamesData = fs.readFileSync(SAMPLE_GAMES_FILE, 'utf8');
    const games = JSON.parse(gamesData);
    
    console.log(`Found ${games.length} games to add...`);
    
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
        } else {
          console.error(`❌ Failed to add: ${game.awayTeam} @ ${game.homeTeam} - Status: ${response.status}`);
        }
      } catch (error) {
        console.error(`❌ Error adding game: ${game.awayTeam} @ ${game.homeTeam} - ${error.message}`);
      }
    }
    
    console.log('Finished adding sample games!');
  } catch (error) {
    console.error('Error reading sample games file:', error.message);
  }
}

// Run the script
addSampleGames();
