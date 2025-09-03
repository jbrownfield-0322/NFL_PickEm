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
  const [showOdds, setShowOdds] = useState(false);
  const [isFetchingOdds, setIsFetchingOdds] = useState(false);
  const { user } = useAuth();

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    fetchGames();
    fetchUserPicks();
    fetchLeagues();
  }, []);

  useEffect(() => {
    if (user) {
      fetchUserPicks();
    }
  }, [selectedLeagueId, user]);

  useEffect(() => {
    fetchGames();
  }, [showOdds]);

  const fetchGames = async () => {
    try {
      const endpoint = showOdds ? '/games/with-odds' : '/games';
      const response = await fetch(`${API_BASE}${endpoint}`);
      if (response.ok) {
        const data = await response.json();
        setGames(data);
      }
    } catch (error) {
      console.error('Error fetching games:', error);
    }
  };

  const fetchOddsForWeek = async () => {
    setIsFetchingOdds(true);
    try {
      const response = await fetch(`${API_BASE}/games/week/${selectedWeek}/fetch-odds`, {
        method: 'POST'
      });
      if (response.ok) {
        // Wait a bit for odds to be fetched, then refresh games
        setTimeout(() => {
          fetchGames();
          setIsFetchingOdds(false);
        }, 5000);
      } else {
        setIsFetchingOdds(false);
        console.error('Error fetching odds');
      }
    } catch (error) {
      console.error('Error fetching odds:', error);
      setIsFetchingOdds(false);
    }
  };

  const fetchUserPicks = async () => {
    if (!user) return;
    
    try {
      const url = selectedLeagueId 
        ? `${API_BASE}/picks/user/${user.id}?leagueId=${selectedLeagueId}`
        : `${API_BASE}/picks/user/${user.id}`;
      
      const response = await fetch(url);
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

  // Filter games by selected week and sort by day/time
  const getFilteredGames = () => {
    const weekGames = games.filter(game => game.week === parseInt(selectedWeek));
    
    // Sort games by day of week and time
    return weekGames.sort((a, b) => {
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
      
      // First sort by day of week
      if (nflDayA !== nflDayB) {
        return nflDayA - nflDayB;
      }
      
      // If same day, sort by time
      return dateA.getTime() - dateB.getTime();
    });
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
          
          <div className="odds-controls">
            <label>
              <input
                type="checkbox"
                checked={showOdds}
                onChange={(e) => setShowOdds(e.target.checked)}
              />
              Show Betting Odds
            </label>
            {showOdds && (
              <button 
                onClick={fetchOddsForWeek}
                disabled={isFetchingOdds}
                className="fetch-odds-button"
              >
                {isFetchingOdds ? 'Fetching...' : 'Fetch Latest Odds'}
              </button>
            )}
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
              {showOdds && (
                <>
                  <th>Spread</th>
                  <th>Total</th>
                  <th>Odds</th>
                </>
              )}
              <th>Current Pick</th>
              <th>Your Pick</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {filteredGames.map(game => {
              const userPickForGame = userPicks.find(pick => pick.game.id === game.id);
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
                  {showOdds && (
                    <>
                      <td data-label="Spread">
                        {game.spread ? (
                          <span className="spread-info">
                            {game.spreadTeam === game.homeTeam ? 'H' : 'A'} {game.spread > 0 ? '+' : ''}{game.spread}
                          </span>
                        ) : (
                          <span className="no-odds">N/A</span>
                        )}
                      </td>
                      <td data-label="Total">
                        {game.total ? (
                          <span className="total-info">{game.total}</span>
                        ) : (
                          <span className="no-odds">N/A</span>
                        )}
                      </td>
                      <td data-label="Odds">
                        {game.homeTeamOdds && game.awayTeamOdds ? (
                          <div className="odds-info">
                            <div>H: {game.homeTeamOdds > 0 ? '+' : ''}{game.homeTeamOdds}</div>
                            <div>A: {game.awayTeamOdds > 0 ? '+' : ''}{game.awayTeamOdds}</div>
                          </div>
                        ) : (
                          <span className="no-odds">N/A</span>
                        )}
                      </td>
                    </>
                  )}
                  <td data-label="Current Pick">
                    {userPickForGame ? (
                      <span className="current-pick">{userPickForGame.pickedTeam}</span>
                    ) : (
                      <span className="no-pick">No pick made</span>
                    )}
                  </td>
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