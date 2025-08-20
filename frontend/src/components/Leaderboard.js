import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';

function Leaderboard() {
  const [weeklyLeaderboard, setWeeklyLeaderboard] = useState([]);
  const [seasonLeaderboard, setSeasonLeaderboard] = useState([]);
  const [leagues, setLeagues] = useState([]);
  const [selectedLeagueId, setSelectedLeagueId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentWeek, setCurrentWeek] = useState(1); // Default, will be fetched
  const [selectedWeek, setSelectedWeek] = useState(1); // New state for selected week, initialize with 1
  const { user } = useAuth();

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    const fetchLeaderboards = async () => {
      if (!user) {
        setLoading(false);
        return;
      }

      try {
        // Fetch current week from backend
        const weekResponse = await fetch(`${API_BASE}/games/currentWeek`);
        if (!weekResponse.ok) {
          throw new Error(`HTTP error! status: ${weekResponse.status}`);
        }
        const weekData = await weekResponse.text();
        const fetchedWeek = parseInt(weekData, 10);
        setCurrentWeek(fetchedWeek);
        // Set selectedWeek to fetchedWeek only if it's the initial load or selectedWeek is not yet set by user
        if (selectedWeek === 1) {
          setSelectedWeek(fetchedWeek);
        }

        // Fetch user's leagues
        const leaguesResponse = await fetch(`${API_BASE}/leagues/user/${user.id}`);
        if (!leaguesResponse.ok) {
          throw new Error(`HTTP error! status: ${leaguesResponse.status}`);
        }
        const leaguesData = await leaguesResponse.json();
        setLeagues(leaguesData);

        // Fetch weekly leaderboard
        const weeklyLeaderboardUrl = selectedLeagueId ? `${API_BASE}/leaderboard/weekly/${selectedWeek}?leagueId=${selectedLeagueId}` : `${API_BASE}/leaderboard/weekly/${selectedWeek}`;
        const weeklyResponse = await fetch(weeklyLeaderboardUrl);
        if (!weeklyResponse.ok) {
          throw new Error(`HTTP error! status: ${weeklyResponse.status}`);
        }
        const weeklyData = await weeklyResponse.json();
        setWeeklyLeaderboard(weeklyData);

        // Fetch season leaderboard
        const seasonLeaderboardUrl = selectedLeagueId ? `${API_BASE}/leaderboard/season?leagueId=${selectedLeagueId}` : `${API_BASE}/leaderboard/season`;
        const seasonResponse = await fetch(seasonLeaderboardUrl);
        if (!seasonResponse.ok) {
          throw new Error(`HTTP error! status: ${seasonResponse.status}`);
        }
        const seasonData = await seasonResponse.json();
        setSeasonLeaderboard(seasonData);

      } catch (error) {
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
    return <div>Error: {error.message}</div>;
  }

  // Assuming a max of 18 regular season weeks
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
              <th>Username</th>
              <th>Score</th>
            </tr>
          </thead>
          <tbody>
            {data.map((player, index) => (
              <tr key={player.username}>
                <td data-label="Rank">{index + 1}</td>
                <td data-label="Username">{player.username}</td>
                <td data-label="Score">{player.score}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );

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
          <label htmlFor="league-select">View Leaderboard For:</label>
          <select id="league-select" value={selectedLeagueId} onChange={(e) => setSelectedLeagueId(e.target.value)}>
            <option value="">Global Leaderboard</option>
            {leagues.map(league => (
              <option key={league.id} value={league.id}>{league.name}</option>
            ))}
          </select>
        </div>
      </div>

      {renderTable(`Weekly Leaderboard (Week ${selectedWeek})`, weeklyLeaderboard)}
      {renderTable("Season Leaderboard", seasonLeaderboard)}
    </div>
  );
}

export default Leaderboard; 