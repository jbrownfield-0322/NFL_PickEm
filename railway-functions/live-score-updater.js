// Railway Function to fetch live NFL scores and update the database
// This can be triggered on a schedule during game days

const API_BASE = process.env.API_BASE || 'https://your-app.railway.app/api';

// You can integrate with various NFL APIs:
// - ESPN API (requires API key)
// - NFL.com API
// - SportsData.io API
// - The Odds API
// For this example, we'll use a mock API response

async function fetchLiveScores() {
  // In a real implementation, you would fetch from an actual NFL API
  // For example:
  // const response = await fetch('https://api.espn.com/v1/sports/football/nfl/scoreboard', {
  //   headers: { 'Authorization': `Bearer ${process.env.ESPN_API_KEY}` }
  // });
  
  // Mock response for demonstration
  return {
    games: [
      {
        awayTeam: "Green Bay Packers",
        homeTeam: "Philadelphia Eagles",
        awayScore: 24,
        homeScore: 31,
        status: "final",
        quarter: 4,
        timeRemaining: "0:00"
      },
      {
        awayTeam: "Pittsburgh Steelers",
        homeTeam: "Atlanta Falcons",
        awayScore: 17,
        homeScore: 24,
        status: "final",
        quarter: 4,
        timeRemaining: "0:00"
      },
      {
        awayTeam: "Dallas Cowboys",
        homeTeam: "Cleveland Browns",
        awayScore: 31,
        homeScore: 14,
        status: "final",
        quarter: 4,
        timeRemaining: "0:00"
      }
    ]
  };
}

async function getUnscoredGames() {
  try {
    const response = await fetch(`${API_BASE}/games`);
    if (!response.ok) {
      throw new Error(`Failed to fetch games: ${response.status}`);
    }
    
    const games = await response.json();
    return games.filter(game => !game.scored);
  } catch (error) {
    console.error('Error fetching unscored games:', error);
    return [];
  }
}

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

async function processLiveScores() {
  try {
    // Fetch live scores from external API
    const liveData = await fetchLiveScores();
    console.log(`Fetched ${liveData.games.length} live games`);
    
    // Get unscored games from our database
    const unscoredGames = await getUnscoredGames();
    console.log(`Found ${unscoredGames.length} unscored games in database`);
    
    let updatedCount = 0;
    
    for (const liveGame of liveData.games) {
      // Only process final games
      if (liveGame.status !== 'final') {
        console.log(`Skipping ${liveGame.awayTeam} @ ${liveGame.homeTeam} - not final`);
        continue;
      }
      
      // Find matching game in our database
      const dbGame = unscoredGames.find(g => 
        g.awayTeam === liveGame.awayTeam && g.homeTeam === liveGame.homeTeam
      );
      
      if (dbGame) {
        // Determine winner
        let winningTeam;
        if (liveGame.awayScore > liveGame.homeScore) {
          winningTeam = liveGame.awayTeam;
        } else if (liveGame.homeScore > liveGame.awayScore) {
          winningTeam = liveGame.homeTeam;
        } else {
          winningTeam = "TIE";
        }
        
        // Update the score
        const updated = await updateGameScore(dbGame.id, winningTeam);
        if (updated) {
          updatedCount++;
        }
      } else {
        console.log(`Game not found in database: ${liveGame.awayTeam} @ ${liveGame.homeTeam}`);
      }
    }
    
    return updatedCount;
  } catch (error) {
    console.error('Error processing live scores:', error);
    throw error;
  }
}

// Main function that Railway will execute
export default async function handler(req, res) {
  try {
    const updatedCount = await processLiveScores();
    res.json({ 
      success: true, 
      message: `Updated ${updatedCount} game scores`,
      updatedCount 
    });
  } catch (error) {
    console.error('Error in live score updater function:', error);
    res.status(500).json({ error: error.message });
  }
}
