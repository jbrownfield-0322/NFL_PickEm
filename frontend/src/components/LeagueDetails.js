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

  const renderWeeklyWinsTable = () => (
    <div className="table-container">
      <h3>Weekly Wins</h3>
      <p className="table-description">
        Tracks the number of weeks each player has won. Ties count as wins for all tied players.
      </p>
      {weeklyWins.length === 0 ? (
        <p>No weekly wins data available.</p>
      ) : (
        <table>
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

      {renderTable(`Weekly Leaderboard (Week ${currentWeek})`, weeklyLeaderboard)}
      {renderTable("Season Leaderboard", seasonLeaderboard)}
      {renderWeeklyWinsTable()}
    </div>
  );
}

export default LeagueDetails; 