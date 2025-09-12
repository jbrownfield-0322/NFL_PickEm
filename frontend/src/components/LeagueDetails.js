import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

function LeagueDetails() {
  const { leagueId } = useParams();
  const [league, setLeague] = useState(null);
  const [weeklyLeaderboard, setWeeklyLeaderboard] = useState([]);
  const [seasonLeaderboard, setSeasonLeaderboard] = useState([]);
  const [weeklyWins, setWeeklyWins] = useState([]);
  const [currentWeek, setCurrentWeek] = useState(1); // Default, will be fetched
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    const fetchLeagueDetails = async () => {
      try {
        // Fetch current week from backend
        const weekResponse = await fetch(`${API_BASE}/games/currentWeek`);
        if (!weekResponse.ok) {
          throw new Error(`HTTP error! status: ${weekResponse.status}`);
        }
        const weekData = await weekResponse.text();
        const fetchedWeek = parseInt(weekData, 10);
        setCurrentWeek(fetchedWeek);

        // Fetch league details
        const leagueResponse = await fetch(`${API_BASE}/leagues/${leagueId}`);
        if (!leagueResponse.ok) {
          throw new Error(`HTTP error! status: ${leagueResponse.status}`);
        }
        const leagueData = await leagueResponse.json();
        setLeague(leagueData);

        // Fetch weekly leaderboard for the league
        const weeklyLeaderboardUrl = `${API_BASE}/leaderboard/weekly/${fetchedWeek}?leagueId=${leagueId}`;
        const weeklyResponse = await fetch(weeklyLeaderboardUrl);
        if (!weeklyResponse.ok) {
          throw new Error(`HTTP error! status: ${weeklyResponse.status}`);
        }
        const weeklyData = await weeklyResponse.json();
        setWeeklyLeaderboard(weeklyData);

        // Fetch season leaderboard for the league
        const seasonLeaderboardUrl = `${API_BASE}/leaderboard/season?leagueId=${leagueId}`;
        const seasonResponse = await fetch(seasonLeaderboardUrl);
        if (!seasonResponse.ok) {
          throw new Error(`HTTP error! status: ${seasonResponse.status}`);
        }
        const seasonData = await seasonResponse.json();
        setSeasonLeaderboard(seasonData);

        // Fetch weekly wins for the league
        const weeklyWinsUrl = `${API_BASE}/leaderboard/weekly-wins?leagueId=${leagueId}`;
        const weeklyWinsResponse = await fetch(weeklyWinsUrl);
        if (!weeklyWinsResponse.ok) {
          throw new Error(`HTTP error! status: ${weeklyWinsResponse.status}`);
        }
        const weeklyWinsData = await weeklyWinsResponse.json();
        setWeeklyWins(weeklyWinsData);

      } catch (error) {
        setError(error);
      } finally {
        setLoading(false);
      }
    };

    fetchLeagueDetails();
  }, [leagueId, API_BASE]);

  if (loading) {
    return <div>Loading league details...</div>;
  }

  if (error) {
    return <div>Error: {error.message}</div>;
  }

  if (!league) {
    return <div>League not found.</div>;
  }

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
                          <span className="no-data">—</span>
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

  return (
    <div className="main-content">
      <h2>League: {league.name}</h2>
      <p>Join Code: <strong>{league.joinCode}</strong></p>
      <p>Admin: {league.admin.name || league.admin.username}</p>

      <h3>Members:</h3>
      <ul className="member-list">
        {league.members.map(member => (
          <li key={member.id}>{member.name || member.username}</li>
        ))}
      </ul>

      {renderCombinedTable()}
    </div>
  );
}

export default LeagueDetails; 