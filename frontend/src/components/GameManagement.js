import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';

function GameManagement() {
  const [games, setGames] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [newGame, setNewGame] = useState({
    week: 1,
    homeTeam: '',
    awayTeam: '',
    kickoffTime: '',
    winningTeam: '',
    scored: false
  });
  const { user } = useAuth();

  // API base URL
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    fetchGames();
  }, []);

  const fetchGames = async () => {
    try {
      const response = await fetch(`${API_BASE}/games`);
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      
      // Sort games by week, then by day of week and time
      const sortedGames = data.sort((a, b) => {
        // First sort by week
        if (a.week !== b.week) {
          return a.week - b.week;
        }
        
        const dateA = new Date(a.kickoffTime);
        const dateB = new Date(b.kickoffTime);
        
        // Get day of week (0 = Sunday, 1 = Monday, ..., 6 = Saturday)
        const dayA = dateA.getDay();
        const dayB = dateB.getDay();
        
        // Convert to NFL week order: Thu(4) -> Fri(5) -> Sat(6) -> Sun(0) -> Mon(1) -> Tue(2) -> Wed(3)
        const nflDayOrder = {
          0: 4, // Sunday
          1: 5, // Monday  
          2: 6, // Tuesday
          3: 7, // Wednesday
          4: 1, // Thursday
          5: 2, // Friday
          6: 3  // Saturday
        };
        
        const nflDayA = nflDayOrder[dayA];
        const nflDayB = nflDayOrder[dayB];
        
        // Then sort by day of week
        if (nflDayA !== nflDayB) {
          return nflDayA - nflDayB;
        }
        
        // If same day, sort by time
        return dateA.getTime() - dateB.getTime();
      });
      
      setGames(sortedGames);
    } catch (error) {
      setError(error);
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setNewGame(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(`${API_BASE}/games`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(newGame),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const savedGame = await response.json();
      setGames(prev => [...prev, savedGame]);
      
      // Reset form
      setNewGame({
        week: 1,
        homeTeam: '',
        awayTeam: '',
        kickoffTime: '',
        winningTeam: '',
        scored: false
      });
    } catch (error) {
      setError(error);
    }
  };

  const updateGameScore = async (gameId, winningTeam) => {
    try {
      const response = await fetch(`${API_BASE}/games/${gameId}/score`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ winningTeam }),
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const updatedGame = await response.json();
      setGames(prev => prev.map(game => 
        game.id === gameId ? updatedGame : game
      ));
    } catch (error) {
      setError(error);
    }
  };

  const formatKickoffTime = (kickoffTime) => {
    if (!kickoffTime) return 'TBD';
    const date = new Date(kickoffTime);
    
    // Get day of week abbreviation (MON, TUE, WED, etc.)
    const dayOfWeek = date.toLocaleDateString('en-US', { weekday: 'short' }).toUpperCase();
    
    // Format the date to show in user's local timezone (without comma)
    const options = {
      month: 'numeric',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    };
    
    const timeString = date.toLocaleString('en-US', options);
    
    // Return format: "MON 9/4 7:20 PM"
    return `${dayOfWeek} ${timeString}`;
  };

  if (loading) {
    return <div>Loading games...</div>;
  }

  if (error) {
    return <div>Error: {error.message}</div>;
  }

  return (
    <div className="main-content">
      <h2>Game Management</h2>
      
      {/* Add New Game Form */}
      <div className="form-container">
        <h3>Add New Game</h3>
        <form onSubmit={handleSubmit}>
          <div>
            <label>Week:</label>
            <input
              type="number"
              name="week"
              value={newGame.week}
              onChange={handleInputChange}
              min="1"
              max="18"
              required
            />
          </div>
          
          <div>
            <label>Away Team:</label>
            <input
              type="text"
              name="awayTeam"
              value={newGame.awayTeam}
              onChange={handleInputChange}
              required
            />
          </div>
          
          <div>
            <label>Home Team:</label>
            <input
              type="text"
              name="homeTeam"
              value={newGame.homeTeam}
              onChange={handleInputChange}
              required
            />
          </div>
          
          <div>
            <label>Kickoff Time (ISO format or yyyy-MM-dd HH:mm:ss):</label>
            <input
              type="text"
              name="kickoffTime"
              value={newGame.kickoffTime}
              onChange={handleInputChange}
              placeholder="2024-09-05T20:20:00Z or 2024-09-05 20:20:00"
            />
          </div>
          
          <div>
            <label>Winning Team (leave empty if not played):</label>
            <input
              type="text"
              name="winningTeam"
              value={newGame.winningTeam}
              onChange={handleInputChange}
            />
          </div>
          
          <div>
            <label>
              <input
                type="checkbox"
                name="scored"
                checked={newGame.scored}
                onChange={handleInputChange}
              />
              Game has been scored
            </label>
          </div>
          
          <button type="submit">Add Game</button>
        </form>
      </div>

      {/* Games List */}
      <div className="table-container">
        <h3>All Games</h3>
        <table>
          <thead>
            <tr>
              <th>Week</th>
              <th>Away Team</th>
              <th>Home Team</th>
              <th>Kickoff</th>
              <th>Winning Team</th>
              <th>Scored</th>
              <th>Action</th>
            </tr>
          </thead>
          <tbody>
            {games.map(game => (
              <tr key={game.id}>
                <td data-label="Week">{game.week}</td>
                <td data-label="Away Team">{game.awayTeam}</td>
                <td data-label="Home Team">{game.homeTeam}</td>
                <td data-label="Kickoff">{formatKickoffTime(game.kickoffTime)}</td>
                <td data-label="Winning Team">{game.winningTeam || 'Not played'}</td>
                <td data-label="Scored">{game.scored ? 'Yes' : 'No'}</td>
                <td data-label="Action">
                  {!game.scored && (
                    <div>
                      <button onClick={() => updateGameScore(game.id, game.awayTeam)}>
                        {game.awayTeam} Wins
                      </button>
                      <button onClick={() => updateGameScore(game.id, game.homeTeam)}>
                        {game.homeTeam} Wins
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default GameManagement;
