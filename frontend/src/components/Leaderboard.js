import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';

function Leaderboard() {
  const [weeklyLeaderboard, setWeeklyLeaderboard] = useState([]);
  const [seasonLeaderboard, setSeasonLeaderboard] = useState([]);
  const [weeklyWins, setWeeklyWins] = useState([]);
  const [leagues, setLeagues] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentWeek, setCurrentWeek] = useState(1);
  const [selectedWeek, setSelectedWeek] = useState(1);
  const [hasInitialized, setHasInitialized] = useState(false);
  const [pickComparison, setPickComparison] = useState([]);
  const [showPickComparison, setShowPickComparison] = useState(false);
  const [isScrollable, setIsScrollable] = useState(false);
  const [isMobile, setIsMobile] = useState(false);
  const [pickDifferences, setPickDifferences] = useState([]);
  const [popupData, setPopupData] = useState(null);
  const [showPopup, setShowPopup] = useState(false);
  const { user } = useAuth();

  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  // Function to calculate pick differences matrix between all players
  const calculatePickDifferencesMatrix = (comparisonData, currentUser) => {
    if (!comparisonData || comparisonData.length === 0 || !currentUser) {
      return { players: [], matrix: [] };
    }

    // Get all unique usernames and create player list
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
    
    const players = Array.from(allUsernames).map(username => ({
      username: username,
      name: usernameToNameMap.get(username) || username,
      isCurrentUser: username === currentUser.username
    })).sort((a, b) => {
      // Put current user first, then sort alphabetically
      if (a.isCurrentUser) return -1;
      if (b.isCurrentUser) return 1;
      return a.name.localeCompare(b.name);
    });
    
    // Create matrix of differences
    const matrix = players.map(rowPlayer => 
      players.map(colPlayer => {
        if (rowPlayer.username === colPlayer.username) {
          return { differences: 0, totalGames: 0, percentage: 0, isSelf: true, differentGames: [] };
        }
        
        let differences = 0;
        let totalGames = 0;
        const differentGames = [];
        
        comparisonData.forEach(game => {
          // Get row player's pick
          let rowPlayerPick;
          if (rowPlayer.username === currentUser.username) {
            rowPlayerPick = game.yourPick;
          } else {
            rowPlayerPick = game.otherPicks.find(p => p.username === rowPlayer.username)?.pickedTeam;
          }
          
          // Get column player's pick
          let colPlayerPick;
          if (colPlayer.username === currentUser.username) {
            colPlayerPick = game.yourPick;
          } else {
            colPlayerPick = game.otherPicks.find(p => p.username === colPlayer.username)?.pickedTeam;
          }
          
          // Only count games where both players made picks
          if (rowPlayerPick && colPlayerPick) {
            totalGames++;
            if (rowPlayerPick !== colPlayerPick) {
              differences++;
              // Store the game details where picks differ
              differentGames.push({
                gameId: game.gameId,
                awayTeam: game.awayTeam,
                homeTeam: game.homeTeam,
                kickoffTime: game.kickoffTime,
                rowPlayerPick: rowPlayerPick,
                colPlayerPick: colPlayerPick,
                gameResult: game.gameResult,
                rowPlayerCorrect: rowPlayerPick === game.gameResult,
                colPlayerCorrect: colPlayerPick === game.gameResult
              });
            }
          }
        });
        
        return {
          differences: differences,
          totalGames: totalGames,
          percentage: totalGames > 0 ? Math.round((differences / totalGames) * 100) : 0,
          isSelf: false,
          differentGames: differentGames
        };
      })
    );
    
    return { players, matrix };
  };

  // Function to show popup with pick differences
  const showPickDifferencesPopup = (rowPlayer, colPlayer, cellData) => {
    if (cellData.isSelf || cellData.differentGames.length === 0) {
      return;
    }
    
    setPopupData({
      rowPlayer: rowPlayer,
      colPlayer: colPlayer,
      differentGames: cellData.differentGames,
      totalDifferences: cellData.differences,
      totalGames: cellData.totalGames
    });
    setShowPopup(true);
  };

  // Function to hide popup
  const hidePopup = () => {
    setShowPopup(false);
    setPopupData(null);
  };

  // Auto-select league when leagues are loaded and user has only one
  useEffect(() => {
    if (leagues.length === 1 && !selectedLeagueId) {
      setSelectedLeagueId(leagues[0].id.toString());
    }
  }, [leagues, selectedLeagueId]);

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
          // Only set selectedWeek to current week on initial load
          if (!hasInitialized) {
            setSelectedWeek(fetchedWeek);
            setHasInitialized(true);
          }
        }

        // Fetch user's leagues
        const leaguesResponse = await fetch(`${API_BASE}/leagues/user/${user.id}`);
        if (leaguesResponse.ok) {
          const leaguesData = await leaguesResponse.json();
          setLeagues(leaguesData);
          
          // Auto-select league if user is only in one league
          if (leaguesData.length === 1 && !selectedLeagueId) {
            setSelectedLeagueId(leaguesData[0].id.toString());
          }
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

        // Fetch weekly wins only if league is selected
        if (selectedLeagueId) {
          const weeklyWinsResponse = await fetch(`${API_BASE}/leaderboard/weekly-wins?leagueId=${parseInt(selectedLeagueId, 10)}`);
          if (weeklyWinsResponse.ok) {
            const weeklyWinsData = await weeklyWinsResponse.json();
            setWeeklyWins(weeklyWinsData);
          }
        } else {
          setWeeklyWins([]);
        }

        // Fetch pick comparison only if league is selected
        if (user && selectedLeagueId) {
          const comparisonResponse = await fetch(`${API_BASE}/picks/comparison/${user.id}/${selectedWeek}?leagueId=${parseInt(selectedLeagueId, 10)}`);
          if (comparisonResponse.ok) {
            const comparisonData = await comparisonResponse.json();
            setPickComparison(comparisonData);
            // Calculate pick differences matrix
            const matrixData = calculatePickDifferencesMatrix(comparisonData, user);
            setPickDifferences(matrixData);
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

  // Function to combine all leaderboard data into one table
  const combineLeaderboardData = () => {
    // Create a map to store combined player data
    const playerMap = new Map();
    
    // Add weekly leaderboard data
    weeklyLeaderboard.forEach((player, index) => {
      const key = player.username;
      if (!playerMap.has(key)) {
        playerMap.set(key, {
          username: player.username,
          name: player.name || player.username,
          weeklyRank: index + 1,
          weeklyScore: player.score,
          seasonRank: null,
          seasonScore: null,
          weeklyWins: null
        });
      } else {
        const existing = playerMap.get(key);
        existing.weeklyRank = index + 1;
        existing.weeklyScore = player.score;
      }
    });
    
    // Add season leaderboard data
    seasonLeaderboard.forEach((player, index) => {
      const key = player.username;
      if (!playerMap.has(key)) {
        playerMap.set(key, {
          username: player.username,
          name: player.name || player.username,
          weeklyRank: null,
          weeklyScore: null,
          seasonRank: index + 1,
          seasonScore: player.score,
          weeklyWins: null
        });
      } else {
        const existing = playerMap.get(key);
        existing.seasonRank = index + 1;
        existing.seasonScore = player.score;
      }
    });
    
    // Add weekly wins data
    weeklyWins.forEach((player, index) => {
      const key = player.username;
      if (!playerMap.has(key)) {
        playerMap.set(key, {
          username: player.username,
          name: player.name || player.username,
          weeklyRank: null,
          weeklyScore: null,
          seasonRank: null,
          seasonScore: null,
          weeklyWins: player.weeklyWins
        });
      } else {
        const existing = playerMap.get(key);
        existing.weeklyWins = player.weeklyWins;
      }
    });
    
    // Convert map to array and sort by season rank (primary) or weekly rank (secondary)
    return Array.from(playerMap.values()).sort((a, b) => {
      // If both have season ranks, sort by season rank
      if (a.seasonRank && b.seasonRank) {
        return a.seasonRank - b.seasonRank;
      }
      // If only one has season rank, prioritize it
      if (a.seasonRank && !b.seasonRank) return -1;
      if (!a.seasonRank && b.seasonRank) return 1;
      // If neither has season rank, sort by weekly rank
      if (a.weeklyRank && b.weeklyRank) {
        return a.weeklyRank - b.weeklyRank;
      }
      // If only one has weekly rank, prioritize it
      if (a.weeklyRank && !b.weeklyRank) return -1;
      if (!a.weeklyRank && b.weeklyRank) return 1;
      // Finally, sort alphabetically by name
      return a.name.localeCompare(b.name);
    });
  };

  const renderCombinedTable = () => {
    const combinedData = combineLeaderboardData();
    
    return (
      <div className="table-container">
        <h3>League Standings</h3>
        <p className="table-description">
          Combined view of weekly scores, season totals, and weekly wins for all players. 
          Weekly wins are only calculated after all games for a week have been completed and scored.
        </p>
        {combinedData.length === 0 ? (
          <p>No leaderboard data available.</p>
        ) : (
          <div className="score-tally-container">
            <div className="score-tally-scroll">
              <table className="score-tally-table">
                <thead>
                  <tr>
                    <th>Player</th>
                    <th>Weekly Score</th>
                    <th>Season Score</th>
                    <th>Weekly Wins</th>
                  </tr>
                </thead>
                <tbody>
                  {combinedData.map((player) => (
                    <tr key={player.username}>
                      <td data-label="Player">
                        <div className="player-info">
                          <div className="player-name">{player.name}</div>
                          <div className="player-username">
                            @{player.username}
                          </div>
                        </div>
                      </td>
                      <td data-label="Weekly Score">
                        {player.weeklyScore !== null ? (
                          <div className="score-with-rank">
                            <div className="score-value">{player.weeklyScore}</div>
                            {player.weeklyRank && (
                              <div className="rank-info">
                                Rank #{player.weeklyRank}
                              </div>
                            )}
                          </div>
                        ) : (
                          <span className="no-data">—</span>
                        )}
                      </td>
                      <td data-label="Season Score">
                        {player.seasonScore !== null ? (
                          <div className="score-with-rank">
                            <div className="score-value">{player.seasonScore}</div>
                            {player.seasonRank && (
                              <div className="rank-info">
                                Rank #{player.seasonRank}
                              </div>
                            )}
                          </div>
                        ) : (
                          <span className="no-data">—</span>
                        )}
                      </td>
                      <td data-label="Weekly Wins">
                        {player.weeklyWins !== null ? (
                          <span className="score-value">{player.weeklyWins}</span>
                        ) : (
                          <span className="no-data" title="Weekly wins are only calculated after all games for completed weeks are scored">
                            —
                          </span>
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

  const renderTable = (title, data) => (
    <div className="table-container">
      <h3>{title}</h3>
      {data.length === 0 ? (
        <p>No data available for this leaderboard.</p>
      ) : (
        <div className="score-tally-container">
          <div className="score-tally-scroll">
            <table className="score-tally-table">
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
          </div>
        </div>
      )}
    </div>
  );

  const renderWeeklyWinsTable = () => (
    <div className="table-container">
      <h3>Weekly Wins</h3>
      <p className="table-description">
        Tracks the number of weeks each player has won. Ties count as wins for all tied players.
      </p>
      {weeklyWins.length === 0 ? (
        <p>No weekly wins data available.</p>
      ) : (
        <div className="score-tally-container">
          <div className="score-tally-scroll">
            <table className="score-tally-table">
              <thead>
                <tr>
                  <th>Rank</th>
                  <th>Name</th>
                  <th>Weekly Wins</th>
                </tr>
              </thead>
              <tbody>
                {weeklyWins.map((player, index) => (
                  <tr key={player.username}>
                    <td data-label="Rank">{index + 1}</td>
                    <td data-label="Name">{player.name || player.username}</td>
                    <td data-label="Weekly Wins">{player.weeklyWins}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
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
    if (!pickDifferences || !pickDifferences.players || pickDifferences.players.length === 0) {
      return null;
    }

    const { players, matrix } = pickDifferences;

    return (
      <div className="table-container">
        <div className="pick-differences-header">
          <h3>Pick Differences Matrix - Week {selectedWeek}</h3>
          <p className="pick-differences-description">
            Shows how many picks differ between each pair of players. Read across rows to see how different each player is from others.
            {isMobile && (
              <span className="mobile-hint">
                <br />
                <strong>💡 Tip:</strong> Scroll horizontally to see all players. Lower numbers = more similar picks.
              </span>
            )}
          </p>
        </div>
        
        <div className="pick-differences-container">
          <div className="pick-differences-scroll">
            <table className="pick-differences-matrix">
              <thead>
                <tr>
                  <th className="matrix-corner-header">
                    <div className="header-content">
                      <span className="header-title">Players</span>
                      <span className="header-subtitle">Row vs Column</span>
                    </div>
                  </th>
                  {players.map((player) => (
                    <th key={player.username} className={`matrix-header ${player.isCurrentUser ? 'current-user-header' : ''}`}>
                      <div className="header-player-info">
                        <span className="header-player-name">{player.name}</span>
                        <span className="header-player-username">@{player.username}</span>
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {players.map((rowPlayer, rowIndex) => (
                  <tr key={rowPlayer.username} className={`matrix-row ${rowPlayer.isCurrentUser ? 'current-user-row' : ''}`}>
                    <td className={`matrix-row-header ${rowPlayer.isCurrentUser ? 'current-user-row-header' : ''}`}>
                      <div className="row-player-info">
                        <span className="row-player-name">{rowPlayer.name}</span>
                        <span className="row-player-username">@{rowPlayer.username}</span>
                      </div>
                    </td>
                    {players.map((colPlayer, colIndex) => {
                      const cellData = matrix[rowIndex][colIndex];
                      return (
                        <td 
                          key={colPlayer.username} 
                          className={`matrix-cell ${rowPlayer.isCurrentUser ? 'current-user-cell' : ''} ${colPlayer.isCurrentUser ? 'current-user-cell' : ''} ${!cellData.isSelf && cellData.differentGames.length > 0 ? 'clickable-cell' : ''}`}
                          onClick={() => showPickDifferencesPopup(rowPlayer, colPlayer, cellData)}
                          onMouseEnter={(e) => {
                            if (!cellData.isSelf && cellData.differentGames.length > 0) {
                              e.target.style.cursor = 'pointer';
                            }
                          }}
                          onMouseLeave={(e) => {
                            e.target.style.cursor = 'default';
                          }}
                        >
                          {cellData.isSelf ? (
                            <div className="self-cell">
                              <span className="self-indicator">—</span>
                            </div>
                          ) : (
                            <div className="difference-cell">
                              <span className={`difference-count ${cellData.differences === 0 ? 'perfect-match' : cellData.differences <= 2 ? 'close-match' : 'different-match'}`}>
                                {cellData.differences}
                              </span>
                              <span className="total-games">/{cellData.totalGames}</span>
                              {cellData.differentGames.length > 0 && (
                                <div className="hover-hint">Click for details</div>
                              )}
                            </div>
                          )}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    );
  };

  // Popup component for showing pick differences
  const renderPickDifferencesPopup = () => {
    if (!showPopup || !popupData) {
      return null;
    }

    const formatKickoffTime = (kickoffTime) => {
      if (!kickoffTime) return 'TBD';
      const date = new Date(kickoffTime);
      return date.toLocaleString('en-US', {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
        timeZoneName: 'short'
      });
    };

    const getPickResultClass = (isCorrect) => {
      return isCorrect ? 'correct-pick' : 'incorrect-pick';
    };

    const getPickResultIcon = (isCorrect) => {
      return isCorrect ? '✅' : '❌';
    };

    return (
      <div className="popup-overlay" onClick={hidePopup}>
        <div className="popup-content" onClick={(e) => e.stopPropagation()}>
          <div className="popup-header">
            <h3>Pick Differences</h3>
            <button className="popup-close" onClick={hidePopup}>×</button>
          </div>
          
          <div className="popup-body">
            <div className="players-comparison">
              <div className="player-info">
                <strong>{popupData.rowPlayer.name}</strong>
                <span className="username">@{popupData.rowPlayer.username}</span>
              </div>
              <div className="vs-separator">vs</div>
              <div className="player-info">
                <strong>{popupData.colPlayer.name}</strong>
                <span className="username">@{popupData.colPlayer.username}</span>
              </div>
            </div>
            
            <div className="summary-stats">
              <p>
                <strong>{popupData.totalDifferences}</strong> different picks out of <strong>{popupData.totalGames}</strong> total games
                ({Math.round((popupData.totalDifferences / popupData.totalGames) * 100)}% disagreement)
              </p>
            </div>

            <div className="differences-list">
              <h4>Games with Different Picks:</h4>
              <div className="score-tally-container">
                <div className="score-tally-scroll">
                  <table className="score-tally-table">
                    <thead>
                      <tr>
                        <th>Game</th>
                        <th>Kickoff</th>
                        <th>{popupData.rowPlayer.name}</th>
                        <th>{popupData.colPlayer.name}</th>
                        <th>Result</th>
                      </tr>
                    </thead>
                    <tbody>
                      {popupData.differentGames.map((game, index) => (
                        <tr key={game.gameId || index}>
                          <td data-label="Game">
                            <div className="player-info">
                              <div className="player-name">
                                {game.awayTeam} @ {game.homeTeam}
                              </div>
                            </div>
                          </td>
                          <td data-label="Kickoff">
                            <span className="score-value">
                              {formatKickoffTime(game.kickoffTime)}
                            </span>
                          </td>
                          <td data-label={popupData.rowPlayer.name}>
                            <div className="pick-display">
                              <span className={`pick-team ${getPickResultClass(game.rowPlayerCorrect)}`}>
                                {getPickResultIcon(game.rowPlayerCorrect)} {game.rowPlayerPick}
                              </span>
                            </div>
                          </td>
                          <td data-label={popupData.colPlayer.name}>
                            <div className="pick-display">
                              <span className={`pick-team ${getPickResultClass(game.colPlayerCorrect)}`}>
                                {getPickResultIcon(game.colPlayerCorrect)} {game.colPlayerPick}
                              </span>
                            </div>
                          </td>
                          <td data-label="Result">
                            <span className="score-value">
                              {game.gameResult || 'TBD'}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
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

      {renderCombinedTable()}
      {renderPickComparisonTable()}
      {renderPickDifferencesTable()}
      {renderPickDifferencesPopup()}
    </div>
  );
}

export default Leaderboard; 