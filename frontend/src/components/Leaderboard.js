import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';

function Leaderboard() {
  const [weeklyLeaderboard, setWeeklyLeaderboard] = useState([]);
  const [seasonLeaderboard, setSeasonLeaderboard] = useState([]);
  const [leagues, setLeagues] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentWeek, setCurrentWeek] = useState(1);
  const [selectedWeek, setSelectedWeek] = useState(1);
  const [pickComparison, setPickComparison] = useState([]);
  const [showPickComparison, setShowPickComparison] = useState(false);
  const { user } = useAuth();

  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    const fetchLeaderboards = async () => {
      if (!user) {
        setLoading(false);
        return;
      }

      try {
        // Fetch current week
        const weekResponse = await fetch(`${API_BASE}/games/currentWeek`);
        if (weekResponse.ok) {
          const weekData = await weekResponse.text();
          const fetchedWeek = parseInt(weekData, 10);
          setCurrentWeek(fetchedWeek);
          if (selectedWeek === 1) {
            setSelectedWeek(fetchedWeek);
          }
        }

        // Fetch user's leagues
        const leaguesResponse = await fetch(`${API_BASE}/leagues/user/${user.id}`);
        if (leaguesResponse.ok) {
          const leaguesData = await leaguesResponse.json();
          setLeagues(leaguesData);
        }

        // Fetch weekly leaderboard only if league is selected
        if (selectedLeagueId) {
          const weeklyResponse = await fetch(`${API_BASE}/leaderboard/weekly/${selectedWeek}?leagueId=${parseInt(selectedLeagueId, 10)}`);
          if (weeklyResponse.ok) {
            const weeklyData = await weeklyResponse.json();
            setWeeklyLeaderboard(weeklyData);
          }
        } else {
          setWeeklyLeaderboard([]);
        }

        // Fetch season leaderboard only if league is selected
        if (selectedLeagueId) {
          const seasonResponse = await fetch(`${API_BASE}/leaderboard/season?leagueId=${parseInt(selectedLeagueId, 10)}`);
          if (seasonResponse.ok) {
            const seasonData = await seasonResponse.json();
            setSeasonLeaderboard(seasonData);
          }
        } else {
          setSeasonLeaderboard([]);
        }

        // Fetch pick comparison only if league is selected
        if (user && selectedLeagueId) {
          const comparisonResponse = await fetch(`${API_BASE}/picks/comparison/${user.id}/${selectedWeek}?leagueId=${parseInt(selectedLeagueId, 10)}`);
          if (comparisonResponse.ok) {
            const comparisonData = await comparisonResponse.json();
            setPickComparison(comparisonData);
          } else {
            setPickComparison([]);
          }
        } else {
          setPickComparison([]);
        }

      } catch (error) {
        console.error('Leaderboard fetch error:', error);
        setError(error);
      } finally {
        setLoading(false);
      }
    };

    fetchLeaderboards();
  }, [user, selectedWeek, selectedLeagueId, API_BASE]);

  if (loading) {
    return <div>Loading leaderboards...</div>;
  }

  if (error) {
    return (
      <div className="main-content">
        <h2>Leaderboards</h2>
        <div style={{ color: 'red', padding: '20px', border: '1px solid red', borderRadius: '5px', margin: '20px 0' }}>
          <h3>Error Loading Leaderboards</h3>
          <p><strong>Error:</strong> {error.message}</p>
          <p>Please check the browser console for more details.</p>
          <button onClick={() => window.location.reload()}>Retry</button>
        </div>
      </div>
    );
  }

  const totalWeeks = 18;

  const renderTable = (title, data) => (
    <div className="table-container">
      <h3>{title}</h3>
      {data.length === 0 ? (
        <p>No data available for this leaderboard.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Rank</th>
                              <th>Name</th>
              <th>Score</th>
            </tr>
          </thead>
          <tbody>
            {data.map((player, index) => (
              <tr key={player.username}>
                <td data-label="Rank">{index + 1}</td>
                <td data-label="Name">{player.name || player.username}</td>
                <td data-label="Score">{player.score}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );

  const renderPickComparisonTable = () => {
    if (pickComparison.length === 0) {
      return null;
    }

    // Get all unique usernames (including current user) and create a name mapping
    const allUsernames = new Set();
    const usernameToNameMap = new Map();
    
    allUsernames.add(user.username); // Add current user
    usernameToNameMap.set(user.username, user.name || user.username);
    
    pickComparison.forEach(game => {
      game.otherPicks.forEach(pick => {
        allUsernames.add(pick.username);
        // Use the name from the backend if available, otherwise fall back to username
        usernameToNameMap.set(pick.username, pick.name || pick.username);
      });
    });
    
    const usernameList = Array.from(allUsernames).sort();

    return (
      <div className="table-container">
        <div className="pick-comparison-header">
          <h3>Pick Comparison - Week {selectedWeek}</h3>
          <p className="pick-comparison-description">
            Compare picks across all players in your league. Each column shows a player's picks, each row shows a game.
          </p>
          <button 
            onClick={() => setShowPickComparison(!showPickComparison)}
            className="toggle-button"
          >
            {showPickComparison ? 'Hide Pick Comparison' : 'Show Pick Comparison'}
          </button>
        </div>
        
        {showPickComparison && (
          <div className="pick-comparison-container">
            <div className="pick-comparison-scroll">
              <table className="pick-comparison-table">
                <thead>
                  <tr>
                    <th className="game-header">
                      <div className="header-content">
                        <span className="header-title">Game</span>
                        <span className="header-subtitle">Away @ Home</span>
                      </div>
                    </th>
                    {usernameList.map(username => (
                      <th key={username} className={`player-header ${username === user.username ? 'current-user' : ''}`}>
                        <div className="header-content">
                          <span className="header-title">
                            {username === user.username ? 'You' : (usernameToNameMap.get(username) || username)}
                          </span>
                          <span className="header-subtitle">
                            {username === user.username ? user.name || user.username : username}
                          </span>
                        </div>
                      </th>
                    ))}
                    <th className="result-header">
                      <div className="header-content">
                        <span className="header-title">Result</span>
                        <span className="header-subtitle">Winner</span>
                      </div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {pickComparison.map((game) => (
                    <tr key={game.gameId} className="game-row">
                      <td className="game-info">
                        <div className="game-details">
                          <div className="game-teams">
                            <span className="away-team">{game.awayTeam}</span>
                            <span className="vs">@</span>
                            <span className="home-team">{game.homeTeam}</span>
                          </div>
                          <div className="game-status">
                            {game.scored ? (
                              <span className="status-final">Final</span>
                            ) : (
                              <span className="status-pending">Not played</span>
                            )}
                          </div>
                        </div>
                      </td>
                      
                      {usernameList.map(username => {
                        let pickData = null;
                        let isCurrentUser = username === user.username;
                        
                        if (isCurrentUser) {
                          pickData = game.yourPick;
                        } else {
                          const otherPick = game.otherPicks.find(p => p.username === username);
                          pickData = otherPick ? otherPick.pickedTeam : null;
                        }
                        
                        const isCorrect = game.scored && pickData === game.winningTeam;
                        const isIncorrect = game.scored && pickData && pickData !== game.winningTeam;
                        
                        return (
                          <td key={username} className={`pick-cell ${isCurrentUser ? 'current-user-pick' : ''}`}>
                            {pickData ? (
                              <div className="pick-container">
                                <span className={`pick-display ${
                                  game.scored 
                                    ? (isCorrect ? 'correct-pick' : 'incorrect-pick')
                                    : 'pending-pick'
                                }`}>
                                  {pickData}
                                </span>
                                {game.scored && (
                                  <span className={`pick-status ${isCorrect ? 'correct' : 'incorrect'}`}>
                                    {isCorrect ? '✓' : '✗'}
                                  </span>
                                )}
                              </div>
                            ) : (
                              <span className="no-pick">No pick</span>
                            )}
                          </td>
                        );
                      })}
                      
                      <td className="result-cell">
                        {game.scored ? (
                          <div className="result-container">
                            <span className="game-result">
                              <strong>{game.winningTeam}</strong>
                            </span>
                            <span className="result-label">Winner</span>
                          </div>
                        ) : (
                          <span className="pending">TBD</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="main-content">
      <h2>Leaderboards</h2>

      <div className="leaderboard-controls">
        <div>
          <label htmlFor="week-select">Select Week:</label>
          <select id="week-select" value={selectedWeek} onChange={(e) => setSelectedWeek(parseInt(e.target.value, 10))}>
            {[...Array(totalWeeks).keys()].map((weekNum) => (
              <option key={weekNum + 1} value={weekNum + 1}>
                Week {weekNum + 1}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="league-select">Select League:</label>
          <select id="league-select" value={selectedLeagueId} onChange={(e) => setSelectedLeagueId(e.target.value)} required>
            <option value="">Select a League</option>
            {leagues.map(league => (
              <option key={league.id} value={league.id}>{league.name}</option>
            ))}
          </select>
        </div>
      </div>

      {renderTable(`Weekly Leaderboard (Week ${selectedWeek})`, weeklyLeaderboard)}
      {renderTable("Season Leaderboard", seasonLeaderboard)}
      {renderPickComparisonTable()}
    </div>
  );
}

export default Leaderboard; 