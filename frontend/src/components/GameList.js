import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';

const GameList = () => {
  const [games, setGames] = useState([]);
  const [userPicks, setUserPicks] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [selectedWeek, setSelectedWeek] = useState(1);
  const [leagues, setLeagues] = useState([]);
  const [pickMessages, setPickMessages] = useState({});
  const [selectedGamePicks, setSelectedGamePicks] = useState({});
  const [bulkSubmitMessage, setBulkSubmitMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { user } = useAuth();

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    fetchGames();
    fetchUserPicks();
    fetchLeagues();
  }, []);

  const fetchGames = async () => {
    try {
      const response = await fetch(`${API_BASE}/games`);
      if (response.ok) {
        const data = await response.json();
        setGames(data);
      }
    } catch (error) {
      console.error('Error fetching games:', error);
    }
  };

  const fetchUserPicks = async () => {
    if (!user) return;
    
    try {
      const response = await fetch(`${API_BASE}/picks/user/${user.id}`);
      if (response.ok) {
        const data = await response.json();
        setUserPicks(data);
        
        // Initialize selectedGamePicks with existing picks
        const initialPicks = {};
        data.forEach(pick => {
          initialPicks[pick.game.id] = pick.pickedTeam;
        });
        setSelectedGamePicks(initialPicks);
      }
    } catch (error) {
      console.error('Error fetching user picks:', error);
    }
  };

  const fetchLeagues = async () => {
    if (!user) return;
    
    try {
      const response = await fetch(`${API_BASE}/leagues/user/${user.id}`);
      if (response.ok) {
        const data = await response.json();
        setLeagues(data);
      }
    } catch (error) {
      console.error('Error fetching leagues:', error);
    }
  };

  const handlePickChange = (gameId, pickedTeam) => {
    setSelectedGamePicks(prev => ({
      ...prev,
      [gameId]: pickedTeam
    }));
  };

  const submitBulkPicks = async () => {
    if (!user) return;

    // Get all games for the selected week that have picks selected
    const weekGames = games.filter(game => game.week === parseInt(selectedWeek));
    const picksToSubmit = [];
    
    for (const game of weekGames) {
      const pickedTeam = selectedGamePicks[game.id];
      if (pickedTeam && !isGameLocked(game.kickoffTime)) {
        picksToSubmit.push({
          gameId: game.id,
          pickedTeam: pickedTeam
        });
      }
    }

    if (picksToSubmit.length === 0) {
      setBulkSubmitMessage('No picks to submit. Please select teams for games that haven\'t started yet.');
      return;
    }

    setIsSubmitting(true);
    setBulkSubmitMessage('');

    try {
      const bulkPickData = {
        userId: user.id,
        leagueId: selectedLeagueId || null,
        picks: picksToSubmit
      };

      console.log('Submitting bulk picks to:', `${API_BASE}/picks/submit-bulk`);
      console.log('Bulk pick data:', bulkPickData);

      const response = await fetch(`${API_BASE}/picks/submit-bulk`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(bulkPickData),
      });

      console.log('Response status:', response.status);

      if (response.ok) {
        const result = await response.json();
        setBulkSubmitMessage(`Successfully submitted ${picksToSubmit.length} picks!`);
        
        // Clear individual pick messages
        setPickMessages({});
        
        // Refresh user picks
        fetchUserPicks();
      } else {
        console.log('Error response status:', response.status);
        let errorMessage = 'Failed to submit picks';
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
          console.log('Error response body:', errorData);
        } catch (e) {
          console.log('Could not parse error response as JSON');
        }
        setBulkSubmitMessage(`Error: ${errorMessage}`);
      }
    } catch (error) {
      console.error('Error submitting bulk picks:', error);
      setBulkSubmitMessage('Error: Failed to submit picks');
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatKickoffTime = (kickoffTime) => {
    if (!kickoffTime) return 'TBD';
    
    const date = new Date(kickoffTime);
    
    // Format the date to show in user's local timezone
    const options = {
      year: 'numeric',
      month: 'numeric',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
      hour12: true
    };
    
    return date.toLocaleString('en-US', options);
  };

  const isGameLocked = (kickoffTime) => {
    if (!kickoffTime) return false;
    const now = new Date();
    const gameTime = new Date(kickoffTime);
    return now >= gameTime;
  };

  // Get unique weeks from games
  const getAvailableWeeks = () => {
    const weeks = [...new Set(games.map(game => game.week))].sort((a, b) => a - b);
    return weeks;
  };

  // Filter games by selected week
  const getFilteredGames = () => {
    return games.filter(game => game.week === parseInt(selectedWeek));
  };

  const filteredGames = getFilteredGames();

  return (
    <div className="game-list-container">
      <h2>NFL Games</h2>
      <div className="game-list-container">
        <div className="league-controls">
          <div>
            <label htmlFor="week-select">Select Week:</label>
            <select 
              id="week-select"
              value={selectedWeek} 
              onChange={(e) => setSelectedWeek(e.target.value)}
            >
              {getAvailableWeeks().map(week => (
                <option key={week} value={week}>
                  Week {week}
                </option>
              ))}
            </select>
          </div>
          
          <div>
            <label htmlFor="league-select">Select League (optional):</label>
            <select 
              id="league-select"
              value={selectedLeagueId} 
              onChange={(e) => setSelectedLeagueId(e.target.value)}
            >
              <option value="">Global Picks (No League)</option>
              {leagues.map(league => (
                <option key={league.id} value={league.id}>
                  {league.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      <div className="table-container">
        <div className="bulk-submit-section">
          <button 
            onClick={submitBulkPicks} 
            disabled={isSubmitting}
            className="bulk-submit-button"
          >
            {isSubmitting ? 'Submitting...' : `Submit All Picks for Week ${selectedWeek}`}
          </button>
          {bulkSubmitMessage && (
            <div className={`bulk-message ${bulkSubmitMessage.includes('Error') ? 'error' : 'success'}`}>
              {bulkSubmitMessage}
            </div>
          )}
        </div>
        
        <table>
          <thead>
            <tr>
              <th>Week</th>
              <th>Away Team</th>
              <th>Home Team</th>
              <th>Kickoff</th>
              <th>Your Pick</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {filteredGames.map(game => {
              const userPickForGame = userPicks.find(pick => 
                pick.game.id === game.id && 
                (selectedLeagueId === "" ? pick.league === null : pick.league?.id === parseInt(selectedLeagueId))
              );
              const hasPicked = !!userPickForGame;
              const isLockedByTime = isGameLocked(game.kickoffTime);
              const isLocked = isLockedByTime;
              const currentPick = selectedGamePicks[game.id] || (userPickForGame?.pickedTeam || '');

              return (
                <tr key={game.id}>
                  <td data-label="Week">{game.week}</td>
                  <td data-label="Away Team">{game.awayTeam}</td>
                  <td data-label="Home Team">{game.homeTeam}</td>
                  <td data-label="Kickoff">{formatKickoffTime(game.kickoffTime)}</td>
                  <td data-label="Your Pick">
                    {isLocked ? (
                      <span className="locked-pick">
                        {hasPicked ? userPickForGame.pickedTeam : 'Picks locked'}
                      </span>
                    ) : (
                      <select
                        value={currentPick}
                        onChange={(e) => handlePickChange(game.id, e.target.value)}
                        disabled={isLocked}
                        className="pick-select"
                      >
                        <option value="">Select a team</option>
                        <option value={game.awayTeam}>{game.awayTeam}</option>
                        <option value={game.homeTeam}>{game.homeTeam}</option>
                      </select>
                    )}
                  </td>
                  <td data-label="Status">
                    {isLocked ? (
                      <span className="status-locked">Locked</span>
                    ) : hasPicked ? (
                      <span className="status-picked">Picked</span>
                    ) : (
                      <span className="status-pending">Pending</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default GameList;