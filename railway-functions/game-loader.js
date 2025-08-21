// Railway Function to load NFL games automatically
// This can be triggered on a schedule or manually

const API_BASE = process.env.API_BASE || 'https://your-app.railway.app/api';

// Sample NFL schedule data - you can expand this or fetch from an external source
const NFL_SCHEDULE_2024 = {
  week1: [
    {
      week: 1,
      awayTeam: "Green Bay Packers",
      homeTeam: "Philadelphia Eagles",
      kickoffTime: "2024-09-05T20:20:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Pittsburgh Steelers",
      homeTeam: "Atlanta Falcons",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Arizona Cardinals",
      homeTeam: "Buffalo Bills",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Tennessee Titans",
      homeTeam: "Chicago Bears",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "New England Patriots",
      homeTeam: "Cincinnati Bengals",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Houston Texans",
      homeTeam: "Indianapolis Colts",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Jacksonville Jaguars",
      homeTeam: "Miami Dolphins",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Minnesota Vikings",
      homeTeam: "New York Giants",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Carolina Panthers",
      homeTeam: "New Orleans Saints",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Tampa Bay Buccaneers",
      homeTeam: "Washington Commanders",
      kickoffTime: "2024-09-08T13:00:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Denver Broncos",
      homeTeam: "Seattle Seahawks",
      kickoffTime: "2024-09-08T16:05:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Las Vegas Raiders",
      homeTeam: "Los Angeles Chargers",
      kickoffTime: "2024-09-08T16:05:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Dallas Cowboys",
      homeTeam: "Cleveland Browns",
      kickoffTime: "2024-09-08T16:25:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Los Angeles Rams",
      homeTeam: "Detroit Lions",
      kickoffTime: "2024-09-08T16:25:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "Baltimore Ravens",
      homeTeam: "Kansas City Chiefs",
      kickoffTime: "2024-09-08T20:20:00Z",
      winningTeam: "",
      scored: false
    },
    {
      week: 1,
      awayTeam: "New York Jets",
      homeTeam: "San Francisco 49ers",
      kickoffTime: "2024-09-09T20:15:00Z",
      winningTeam: "",
      scored: false
    }
  ]
  // Add more weeks as needed
};

async function loadGamesForWeek(weekNumber) {
  const weekKey = `week${weekNumber}`;
  const games = NFL_SCHEDULE_2024[weekKey];
  
  if (!games) {
    console.log(`No games found for week ${weekNumber}`);
    return;
  }
  
  console.log(`Loading ${games.length} games for week ${weekNumber}...`);
  
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
}

// Main function that Railway will execute
export default async function handler(req, res) {
  try {
    const { week } = req.query || {};
    
    if (week) {
      await loadGamesForWeek(parseInt(week));
      res.json({ success: true, message: `Loaded games for week ${week}` });
    } else {
      // Load all available weeks
      for (const weekKey of Object.keys(NFL_SCHEDULE_2024)) {
        const weekNumber = parseInt(weekKey.replace('week', ''));
        await loadGamesForWeek(weekNumber);
      }
      res.json({ success: true, message: 'Loaded all available games' });
    }
  } catch (error) {
    console.error('Error in game loader function:', error);
    res.status(500).json({ error: error.message });
  }
}
