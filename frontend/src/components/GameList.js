import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';

const GameList = () => {
  const [games, setGames] = useState([]);
  const [userPicks, setUserPicks] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [selectedWeek, setSelectedWeek] = useState('all');
  const [leagues, setLeagues] = useState([]);
  const [pickMessages, setPickMessages] = useState({});
  const [selectedGamePicks, setSelectedGamePicks] = useState({});
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

  const submitPick = async (gameId, pickedTeam) => {
    if (!user) return;

    try {
      const pickData = {
        userId: user.id,
        gameId: gameId,
        pickedTeam: pickedTeam,
        leagueId: selectedLeagueId || null
      };

      console.log('Submitting pick to:', `${API_BASE}/picks/submit`);
      console.log('Pick data:', pickData);

      const response = await fetch(`${API_BASE}/picks/submit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(pickData),
      });

      console.log('Response status:', response.status);
      console.log('Response headers:', response.headers);

      if (response.ok) {
        const result = await response.json();
        
        // Update pick messages
        setPickMessages(prev => ({
          ...prev,
          [gameId]: `Pick submitted: ${pickedTeam}`
        }));

        // Refresh user picks
        fetchUserPicks();
      } else {
        console.log('Error response status:', response.status);
        let errorMessage = 'Failed to submit pick';
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
          console.log('Error response body:', errorData);
        } catch (e) {
          console.log('Could not parse error response as JSON');
        }
        setPickMessages(prev => ({
          ...prev,
          [gameId]: `Error: ${errorMessage}`
        }));
      }
    } catch (error) {
      setPickMessages(prev => ({
        ...prev,
        [gameId]: 'Error: Failed to submit pick'
      }));
    }
  };

  const formatKickoffTime = (kickoffTime) => {
    if (!kickoffTime) return 'TBD';
    const date = new Date(kickoffTime);
    return date.toLocaleString();
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
    if (selectedWeek === 'all') {
      return games;
    }
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
              <option value="all">All Weeks</option>
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
        <table>
          <thead>
            <tr>
              <th>Week</th>
              <th>Date</th>
              <th>Away Team</th>
              <th>Home Team</th>
              <th>Kickoff</th>
              <th>Your Pick</th>
              <th>Action</th>
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
                  <td data-label="Date">{formatKickoffTime(game.kickoffTime)}</td>
                  <td data-label="Away Team">{game.awayTeam}</td>
                  <td data-label="Home Team">{game.homeTeam}</td>
                  <td data-label="Kickoff">{formatKickoffTime(game.kickoffTime)}</td>
                  <td data-label="Your Pick">
                    {isLocked ? (
                      <p>{pickMessages[game.id] || (hasPicked ? `Your pick: ${userPickForGame.pickedTeam}` : 'Picks are locked for this game.')}</p>
                    ) : (
                      <form onSubmit={(e) => {
                        e.preventDefault();
                        if (currentPick) {
                          submitPick(game.id, currentPick);
                        }
                      }}>
                        <select
                          value={currentPick}
                          onChange={(e) => handlePickChange(game.id, e.target.value)}
                          disabled={isLocked}
                        >
                          <option value="">Select a team</option>
                          <option value={game.awayTeam}>{game.awayTeam}</option>
                          <option value={game.homeTeam}>{game.homeTeam}</option>
                        </select>
                        <button type="submit" disabled={!currentPick || isLocked}>
                          Submit Pick
                        </button>
                      </form>
                    )}
                  </td>
                  <td data-label="Action">
                    {pickMessages[game.id] && <p>{pickMessages[game.id]}</p>}
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