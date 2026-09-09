import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../AuthContext';
import { fetchSeasonOptions, appendSeasonParam } from '../utils/season';

const GameList = () => {
  const [games, setGames] = useState([]);
  const [userPicks, setUserPicks] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [selectedSeason, setSelectedSeason] = useState(null);
  const [availableSeasons, setAvailableSeasons] = useState([]);
  const [selectedWeek, setSelectedWeek] = useState(1);
  const [leagues, setLeagues] = useState([]);
  const [pickMessages, setPickMessages] = useState({});
  const [selectedGamePicks, setSelectedGamePicks] = useState({});
  const [bulkSubmitMessage, setBulkSubmitMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [hasInitializedWeek, setHasInitializedWeek] = useState(false);
  const { user } = useAuth();

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    const initSeasons = async () => {
      try {
        const { seasons, currentSeason } = await fetchSeasonOptions(API_BASE);
        setAvailableSeasons(seasons);
        setSelectedSeason(currentSeason);
      } catch (error) {
        console.error('Error fetching seasons:', error);
        const fallback = new Date().getFullYear();
        setAvailableSeasons([fallback]);
        setSelectedSeason(fallback);
      }
    };
    initSeasons();
    fetchLeagues();
  }, [API_BASE]);

  useEffect(() => {
    if (selectedSeason == null) return;
    setHasInitializedWeek(false);
    fetchGames(selectedSeason);
  }, [selectedSeason]);

  // Fetch current week and set as default
  const fetchCurrentWeek = useCallback(async () => {
    if (hasInitializedWeek || selectedSeason == null) return;
    
    try {
      const response = await fetch(appendSeasonParam(`${API_BASE}/games/currentWeek`, selectedSeason));
      if (response.ok) {
        const weekData = await response.text();
        const fetchedWeek = parseInt(weekData, 10);
        if (fetchedWeek) {
          const availableWeeks = [...new Set(games.map(game => game.week))].sort((a, b) => a - b);
          if (availableWeeks.includes(fetchedWeek)) {
            setSelectedWeek(fetchedWeek);
          } else if (availableWeeks.length > 0) {
            setSelectedWeek(availableWeeks[0]);
          }
          setHasInitializedWeek(true);
        }
      }
    } catch (error) {
      console.error('Error fetching current week:', error);
      if (games.length > 0) {
        const availableWeeks = [...new Set(games.map(game => game.week))].sort((a, b) => a - b);
        if (availableWeeks.length > 0) {
          setSelectedWeek(availableWeeks[0]);
          setHasInitializedWeek(true);
        }
      }
    }
  }, [games, hasInitializedWeek, API_BASE, selectedSeason]);

  // Set current week as default when games are loaded
  useEffect(() => {
    if (games.length > 0 && !hasInitializedWeek) {
      fetchCurrentWeek();
    }
  }, [games, hasInitializedWeek, fetchCurrentWeek]);

  // Auto-select league when leagues are loaded and user has only one
  useEffect(() => {
    if (leagues.length === 1 && !selectedLeagueId) {
      setSelectedLeagueId(leagues[0].id.toString());
    }
  }, [leagues, selectedLeagueId]);

  useEffect(() => {
    if (user) {
      fetchUserPicks();
    }
  }, [selectedLeagueId, user]);

  const fetchGames = async (seasonYear) => {
    try {
      const response = await fetch(appendSeasonParam(`${API_BASE}/games`, seasonYear));
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
        
        // Auto-select league if user is only in one league
        if (data.length === 1 && !selectedLeagueId) {
          setSelectedLeagueId(data[0].id.toString());
        }
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

  const formatSpread = (game) => {
    if (!game.primaryOdds || game.primaryOdds.spread === null) {
      return <span className="spread-cell">N/A</span>;
    }
    
    const spread = game.primaryOdds.spread;
    const spreadTeam = game.primaryOdds.spreadTeam;
    const homeTeam = game.homeTeam;
    const awayTeam = game.awayTeam;
    
    // Normalize team names for comparison (case-insensitive, trim whitespace)
    const normalizeTeamName = (name) => {
      if (!name) return '';
      return name.toLowerCase().trim().replace(/\s+/g, ' ');
    };
    
    const normalizedSpreadTeam = normalizeTeamName(spreadTeam);
    const normalizedHomeTeam = normalizeTeamName(homeTeam);
    const normalizedAwayTeam = normalizeTeamName(awayTeam);
    
    // Determine if spread team matches home or away team
    const spreadTeamIsHome = normalizedSpreadTeam === normalizedHomeTeam;
    const spreadTeamIsAway = normalizedSpreadTeam === normalizedAwayTeam;
    
    // Convert spread to home team perspective
    let homeTeamSpread;
    if (spreadTeamIsHome) {
      // If the spread team is the home team, keep the spread as is
      homeTeamSpread = spread;
    } else if (spreadTeamIsAway) {
      // If the spread team is the away team, flip the sign for home team perspective
      homeTeamSpread = -spread;
    } else {
      // Team name mismatch - log for debugging and default to showing spread as-is
      console.warn(`Spread team "${spreadTeam}" doesn't match home "${homeTeam}" or away "${awayTeam}"`);
      homeTeamSpread = spread;
    }
    
    // Format the spread (always show negative for favorites, positive for underdogs)
    const spreadText = homeTeamSpread > 0 ? `+${homeTeamSpread}` : homeTeamSpread.toString();
    
    return (
      <span className="spread-cell">
        {spreadText}
      </span>
    );
  };

  // Get unique weeks from games
  const getAvailableWeeks = () => {
    const weeks = [...new Set(games.map(game => game.week))].sort((a, b) => a - b);
    return weeks;
  };

  // Filter games by selected week and sort chronologically by kickoff
  const getFilteredGames = () => {
    const weekGames = games.filter(game => game.week === parseInt(selectedWeek));
    return weekGames.sort((a, b) => new Date(a.kickoffTime) - new Date(b.kickoffTime));
  };

  const filteredGames = getFilteredGames();

  return (
    <div className="game-list-container">
      <h2>NFL Games</h2>
      <div className="game-list-container">
        <div className="league-controls">
          <div>
            <label htmlFor="season-select">Season:</label>
            <select
              id="season-select"
              value={selectedSeason ?? ''}
              onChange={(e) => setSelectedSeason(parseInt(e.target.value, 10))}
            >
              {availableSeasons.map((year) => (
                <option key={year} value={year}>
                  {year} Season
                </option>
              ))}
            </select>
          </div>

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
              required
            >
              <option value="">Select a League</option>
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
              <th>Spread</th>
              <th>Kickoff</th>
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
                  <td data-label="Spread">{formatSpread(game)}</td>
                  <td data-label="Kickoff">{formatKickoffTime(game.kickoffTime)}</td>
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