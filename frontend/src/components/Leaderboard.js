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
  const [isScrollable, setIsScrollable] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [pickDifferences, setPickDifferences] = useState([]);
  const { user } = useAuth();

  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  // Function to calculate pick differences between current user and other players
  const calculatePickDifferences = (comparisonData, currentUser) => {
    if (!comparisonData || comparisonData.length === 0 || !currentUser) {
      return [];
    }

    // Get all unique usernames
    const allUsernames = new Set();
    const usernameToNameMap = new Map();
    
    allUsernames.add(currentUser.username);
    usernameToNameMap.set(currentUser.username, currentUser.name || currentUser.username);
    
    comparisonData.forEach(game => {
      game.otherPicks.forEach(pick => {
        allUsernames.add(pick.username);
        usernameToNameMap.set(pick.username, pick.name || pick.username);
      });
    });
    
    const otherUsers = Array.from(allUsernames).filter(username => username !== currentUser.username);
    
    return otherUsers.map(otherUsername => {
      let differences = 0;
      let totalGames = 0;
      
      comparisonData.forEach(game => {
        // Get current user's pick
        const currentUserPick = game.yourPick;
        
        // Get other user's pick
        const otherUserPick = game.otherPicks.find(p => p.username === otherUsername)?.pickedTeam;
        
        // Only count games where both users made picks
        if (currentUserPick && otherUserPick) {
          totalGames++;
          if (currentUserPick !== otherUserPick) {
            differences++;
          }
        }
      });
      
      return {
        username: otherUsername,
        name: usernameToNameMap.get(otherUsername) || otherUsername,
        differences: differences,
        totalGames: totalGames,
        percentage: totalGames > 0 ? Math.round((differences / totalGames) * 100) : 0
      };
    }).sort((a, b) => a.differences - b.differences); // Sort by fewest differences first
  };

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
            // Calculate pick differences
            const differences = calculatePickDifferences(comparisonData, user);
            setPickDifferences(differences);
          } else {
            setPickComparison([]);
            setPickDifferences([]);
          }
        } else {
          setPickComparison([]);
          setPickDifferences([]);
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

  // Handle scrollable detection for mobile
  useEffect(() => {
    const checkScrollable = () => {
      const scrollContainer = document.querySelector('.pick-comparison-scroll');
      if (scrollContainer) {
        const isScrollable = scrollContainer.scrollWidth > scrollContainer.clientWidth;
        setIsScrollable(isScrollable);
        
        // Add/remove scrollable class for styling
        if (isScrollable) {
          scrollContainer.classList.add('scrollable');
        } else {
          scrollContainer.classList.remove('scrollable');
        }
      }
    };

    // Check on mount and when comparison is shown
    if (showPickComparison) {
      // Use setTimeout to ensure DOM is updated
      setTimeout(checkScrollable, 100);
    }

    // Check on window resize
    window.addEventListener('resize', checkScrollable);
    return () => window.removeEventListener('resize', checkScrollable);
  }, [showPickComparison, pickComparison]);

  // Add touch scroll indicators for mobile
  useEffect(() => {
    if (!isMobile || !showPickComparison) return;

    const scrollContainer = document.querySelector('.pick-comparison-scroll');
    if (!scrollContainer) return;

    let scrollTimeout;
    const showScrollHint = () => {
      const hint = scrollContainer.querySelector('.scroll-hint');
      if (hint) {
        hint.style.opacity = '1';
        clearTimeout(scrollTimeout);
        scrollTimeout = setTimeout(() => {
          hint.style.opacity = '0';
        }, 2000);
      }
    };

    const handleScroll = () => {
      const hint = scrollContainer.querySelector('.scroll-hint');
      if (hint) {
        hint.style.opacity = '0';
      }
    };

    scrollContainer.addEventListener('touchstart', showScrollHint);
    scrollContainer.addEventListener('scroll', handleScroll);

    return () => {
      scrollContainer.removeEventListener('touchstart', showScrollHint);
      scrollContainer.removeEventListener('scroll', handleScroll);
      clearTimeout(scrollTimeout);
    };
  }, [isMobile, showPickComparison]);

  // Handle mobile detection
  useEffect(() => {
    const checkMobile = () => {
      setIsMobile(window.innerWidth <= 768);
    };

    checkMobile();
    window.addEventListener('resize', checkMobile);
    return () => window.removeEventListener('resize', checkMobile);
  }, []);

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
            {isMobile && (
              <span className="mobile-hint">
                <br />
                <strong>💡 Mobile tip:</strong> Your picks are highlighted in green.
              </span>
            )}
          </p>
          <button 
            onClick={() => setShowPickComparison(!showPickComparison)}
            className="toggle-button"
            aria-expanded={showPickComparison}
            aria-controls="pick-comparison-table"
          >
            {showPickComparison ? 'Hide Pick Comparison' : 'Show Pick Comparison'}
          </button>
        </div>
        
        {showPickComparison && (
          <div className="pick-comparison-container">
            <div className="pick-comparison-scroll">
              <table className="pick-comparison-table" id="pick-comparison-table">
                <thead>
                  <tr>
                    <th className="game-header">
                      <div className="header-content">
                        <span className="header-title">Matchup</span>
                        <span className="header-subtitle">Away @ Home</span>
                      </div>
                    </th>
                    {usernameList.map(username => (
                      <th key={username} className={`player-header ${username === user.username ? 'current-user' : ''}`}>
                        <div className="header-content">
                          <span className="header-title">
                            {username === user.username ? 'You' : (usernameToNameMap.get(username) || username)}
                          </span>
                          <span className="header-subtitle">Pick</span>
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

  const renderPickDifferencesTable = () => {
    if (pickDifferences.length === 0) {
      return null;
    }

    return (
      <div className="table-container">
        <div className="pick-differences-header">
          <h3>Pick Differences - Week {selectedWeek}</h3>
          <p className="pick-differences-description">
            Shows how many picks you have different from each player in your league.
            {isMobile && (
              <span className="mobile-hint">
                <br />
                <strong>💡 Tip:</strong> Lower numbers mean you have more similar picks to that player.
              </span>
            )}
          </p>
        </div>
        
        <div className="pick-differences-container">
          <table className="pick-differences-table">
            <thead>
              <tr>
                <th className="player-diff-header">Player</th>
                <th className="differences-header">Different Picks</th>
                <th className="total-header">Total Games</th>
                <th className="percentage-header">% Different</th>
              </tr>
            </thead>
            <tbody>
              {pickDifferences.map((player) => (
                <tr key={player.username} className="difference-row">
                  <td className="player-diff-cell">
                    <div className="player-info">
                      <span className="player-name">{player.name}</span>
                      <span className="player-username">@{player.username}</span>
                    </div>
                  </td>
                  <td className="differences-cell">
                    <span className={`difference-count ${player.differences === 0 ? 'perfect-match' : player.differences <= 2 ? 'close-match' : 'different-match'}`}>
                      {player.differences}
                    </span>
                  </td>
                  <td className="total-cell">
                    <span className="total-count">{player.totalGames}</span>
                  </td>
                  <td className="percentage-cell">
                    <span className={`percentage ${player.percentage === 0 ? 'perfect-match' : player.percentage <= 25 ? 'close-match' : 'different-match'}`}>
                      {player.percentage}%
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
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
      {renderPickDifferencesTable()}
    </div>
  );
}

export default Leaderboard; 