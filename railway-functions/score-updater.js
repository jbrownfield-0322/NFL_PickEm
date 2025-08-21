// Railway Function to automatically update game scores
// This can be triggered on a schedule after games finish

const API_BASE = process.env.API_BASE || 'https://your-app.railway.app/api';

// Sample game results - you can expand this or integrate with a live scoring API
const GAME_RESULTS = {
  // Week 1 results (example)
  "2024-09-05": [
    {
      awayTeam: "Green Bay Packers",
      homeTeam: "Philadelphia Eagles",
      awayScore: 24,
      homeScore: 31,
      winningTeam: "Philadelphia Eagles"
    }
  ],
  "2024-09-08": [
    {
      awayTeam: "Pittsburgh Steelers",
      homeTeam: "Atlanta Falcons",
      awayScore: 17,
      homeScore: 24,
      winningTeam: "Atlanta Falcons"
    },
    {
      awayTeam: "Arizona Cardinals",
      homeTeam: "Buffalo Bills",
      awayScore: 21,
      homeScore: 28,
      winningTeam: "Buffalo Bills"
    },
    {
      awayTeam: "Tennessee Titans",
      homeTeam: "Chicago Bears",
      awayScore: 27,
      homeScore: 20,
      winningTeam: "Tennessee Titans"
    },
    {
      awayTeam: "New England Patriots",
      homeTeam: "Cincinnati Bengals",
      awayScore: 14,
      homeScore: 31,
      winningTeam: "Cincinnati Bengals"
    },
    {
      awayTeam: "Houston Texans",
      homeTeam: "Indianapolis Colts",
      awayScore: 21,
      homeScore: 17,
      winningTeam: "Houston Texans"
    },
    {
      awayTeam: "Jacksonville Jaguars",
      homeTeam: "Miami Dolphins",
      awayScore: 24,
      homeScore: 31,
      winningTeam: "Miami Dolphins"
    },
    {
      awayTeam: "Minnesota Vikings",
      homeTeam: "New York Giants",
      awayScore: 28,
      homeScore: 24,
      winningTeam: "Minnesota Vikings"
    },
    {
      awayTeam: "Carolina Panthers",
      homeTeam: "New Orleans Saints",
      awayScore: 17,
      homeScore: 24,
      winningTeam: "New Orleans Saints"
    },
    {
      awayTeam: "Tampa Bay Buccaneers",
      homeTeam: "Washington Commanders",
      awayScore: 20,
      homeScore: 17,
      winningTeam: "Tampa Bay Buccaneers"
    },
    {
      awayTeam: "Denver Broncos",
      homeTeam: "Seattle Seahawks",
      awayScore: 16,
      homeScore: 17,
      winningTeam: "Seattle Seahawks"
    },
    {
      awayTeam: "Las Vegas Raiders",
      homeTeam: "Los Angeles Chargers",
      awayScore: 21,
      homeScore: 24,
      winningTeam: "Los Angeles Chargers"
    },
    {
      awayTeam: "Dallas Cowboys",
      homeTeam: "Cleveland Browns",
      awayScore: 31,
      homeScore: 14,
      winningTeam: "Dallas Cowboys"
    },
    {
      awayTeam: "Los Angeles Rams",
      homeTeam: "Detroit Lions",
      awayScore: 24,
      homeScore: 31,
      winningTeam: "Detroit Lions"
    },
    {
      awayTeam: "Baltimore Ravens",
      homeTeam: "Kansas City Chiefs",
      awayScore: 28,
      homeScore: 35,
      winningTeam: "Kansas City Chiefs"
    }
  ],
  "2024-09-09": [
    {
      awayTeam: "New York Jets",
      homeTeam: "San Francisco 49ers",
      awayScore: 17,
      homeScore: 24,
      winningTeam: "San Francisco 49ers"
    }
  ]
};

async function getGamesForDate(date) {
  try {
    const response = await fetch(`${API_BASE}/games`);
    if (!response.ok) {
      throw new Error(`Failed to fetch games: ${response.status}`);
    }
    
    const games = await response.json();
    const targetDate = new Date(date);
    
    // Filter games for the specific date
    return games.filter(game => {
      if (!game.kickoffTime) return false;
      const gameDate = new Date(game.kickoffTime);
      return gameDate.toDateString() === targetDate.toDateString();
    });
  } catch (error) {
    console.error('Error fetching games:', error);
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

async function updateScoresForDate(date) {
  const results = GAME_RESULTS[date];
  if (!results) {
    console.log(`No results found for date: ${date}`);
    return;
  }
  
  const games = await getGamesForDate(date);
  console.log(`Found ${games.length} games for ${date}`);
  
  for (const result of results) {
    // Find matching game
    const game = games.find(g => 
      g.awayTeam === result.awayTeam && g.homeTeam === result.homeTeam
    );
    
    if (game && !game.scored) {
      await updateGameScore(game.id, result.winningTeam);
    } else if (game && game.scored) {
      console.log(`Game already scored: ${result.awayTeam} @ ${result.homeTeam}`);
    } else {
      console.log(`Game not found: ${result.awayTeam} @ ${result.homeTeam}`);
    }
  }
}

// Main function that Railway will execute
export default async function handler(req, res) {
  try {
    const { date } = req.query || {};
    
    if (date) {
      await updateScoresForDate(date);
      res.json({ success: true, message: `Updated scores for ${date}` });
    } else {
      // Update scores for today
      const today = new Date().toISOString().split('T')[0];
      await updateScoresForDate(today);
      res.json({ success: true, message: `Updated scores for today (${today})` });
    }
  } catch (error) {
    console.error('Error in score updater function:', error);
    res.status(500).json({ error: error.message });
  }
}
