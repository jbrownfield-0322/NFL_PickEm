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
  const [pickComparison, setPickComparison] = useState([]);
  const [showPickComparison, setShowPickComparison] = useState(false);
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
        console.log('Fetching current week...');
        const weekResponse = await fetch(`${API_BASE}/games/currentWeek`);
        console.log('Week response status:', weekResponse.status);
        if (!weekResponse.ok) {
          const errorText = await weekResponse.text();
          console.error('Week response error:', errorText);
          throw new Error(`Failed to fetch current week: HTTP ${weekResponse.status} - ${errorText}`);
        }
        const weekData = await weekResponse.text();
        console.log('Week data:', weekData);
        const fetchedWeek = parseInt(weekData, 10);
        setCurrentWeek(fetchedWeek);
        // Set selectedWeek to fetchedWeek only if it's the initial load or selectedWeek is not yet set by user
        if (selectedWeek === 1) {
          setSelectedWeek(fetchedWeek);
        }

        // Fetch user's leagues
        console.log('Fetching user leagues...');
        const leaguesResponse = await fetch(`${API_BASE}/leagues/user/${user.id}`);
        console.log('Leagues response status:', leaguesResponse.status);
        if (!leaguesResponse.ok) {
          const errorText = await leaguesResponse.text();
          console.error('Leagues response error:', errorText);
          throw new Error(`Failed to fetch leagues: HTTP ${leaguesResponse.status} - ${errorText}`);
        }
        const leaguesData = await leaguesResponse.json();
        console.log('Leagues data:', leaguesData);
        setLeagues(leaguesData);

        // Fetch weekly leaderboard
        console.log('Fetching weekly leaderboard...');
        if (!selectedLeagueId) {
          console.log('No league selected, skipping weekly leaderboard fetch');
          setWeeklyLeaderboard([]);
        } else {
          const weeklyLeaderboardUrl = `${API_BASE}/leaderboard/weekly/${selectedWeek}?leagueId=${parseInt(selectedLeagueId, 10)}`;
        console.log('Weekly leaderboard URL:', weeklyLeaderboardUrl);
        const weeklyResponse = await fetch(weeklyLeaderboardUrl);
        console.log('Weekly response status:', weeklyResponse.status);
        if (!weeklyResponse.ok) {
          const errorText = await weeklyResponse.text();
          console.error('Weekly response error:', errorText);
          throw new Error(`Failed to fetch weekly leaderboard: HTTP ${weeklyResponse.status} - ${errorText}`);
        }
        const weeklyData = await weeklyResponse.json();
        console.log('Weekly data:', weeklyData);
        setWeeklyLeaderboard(weeklyData);

        // Fetch season leaderboard
        console.log('Fetching season leaderboard...');
        if (!selectedLeagueId) {
          console.log('No league selected, skipping season leaderboard fetch');
          setSeasonLeaderboard([]);
        } else {
          const seasonLeaderboardUrl = `${API_BASE}/leaderboard/season?leagueId=${parseInt(selectedLeagueId, 10)}`;
        console.log('Season leaderboard URL:', seasonLeaderboardUrl);
        const seasonResponse = await fetch(seasonLeaderboardUrl);
        console.log('Season response status:', seasonResponse.status);
        if (!seasonResponse.ok) {
          const errorText = await seasonResponse.text();
          console.error('Season response error:', errorText);
          throw new Error(`Failed to fetch season leaderboard: HTTP ${seasonResponse.status} - ${errorText}`);
        }
        const seasonData = await seasonResponse.json();
        console.log('Season data:', seasonData);
        setSeasonLeaderboard(seasonData);

        // Fetch pick comparison data
        if (user && selectedLeagueId) {
          console.log('Fetching pick comparison...');
          const comparisonUrl = `${API_BASE}/picks/comparison/${user.id}/${selectedWeek}?leagueId=${parseInt(selectedLeagueId, 10)}`;
          console.log('Comparison URL:', comparisonUrl);
          const comparisonResponse = await fetch(comparisonUrl);
          console.log('Comparison response status:', comparisonResponse.status);
          if (comparisonResponse.ok) {
            const comparisonData = await comparisonResponse.json();
            console.log('Comparison data:', comparisonData);
            setPickComparison(comparisonData);
          } else {
            console.error('Failed to fetch pick comparison data');
            setPickComparison([]);
          }
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

  const renderPickComparisonTable = () => (
    <div className="table-container">
      <h3>Pick Comparison - Week {selectedWeek}</h3>
      <button 
        onClick={() => setShowPickComparison(!showPickComparison)}
        className="toggle-button"
        style={{ marginBottom: '10px' }}
      >
        {showPickComparison ? 'Hide Pick Comparison' : 'Show Pick Comparison'}
      </button>
      
      {showPickComparison && (
        pickComparison.length === 0 ? (
          <p>No pick comparison data available for this week.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Game</th>
                <th>Your Pick</th>
                <th>Other Picks</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              {pickComparison.map((game) => (
                <tr key={game.gameId}>
                  <td data-label="Game">
                    <strong>{game.awayTeam} @ {game.homeTeam}</strong>
                  </td>
                  <td data-label="Your Pick">
                    {game.yourPick ? (
                      <span className={game.scored && game.yourPick === game.winningTeam ? 'correct-pick' : 
                                      game.scored && game.yourPick !== game.winningTeam ? 'incorrect-pick' : 'pending-pick'}>
                        {game.yourPick}
                      </span>
                    ) : (
                      <span className="no-pick">No pick</span>
                    )}
                  </td>
                  <td data-label="Other Picks">
                    {game.otherPicks.length > 0 ? (
                      <div className="other-picks">
                        {game.otherPicks.map((pick, index) => (
                          <div key={index} className="pick-item">
                            <span className="username">{pick.username}:</span>
                            <span className={game.scored && pick.pickedTeam === game.winningTeam ? 'correct-pick' : 
                                            game.scored && pick.pickedTeam !== game.winningTeam ? 'incorrect-pick' : 'pending-pick'}>
                              {pick.pickedTeam}
                            </span>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <span className="no-picks">No other picks</span>
                    )}
                  </td>
                  <td data-label="Result">
                    {game.scored ? (
                      <span className="game-result">
                        <strong>{game.winningTeam} wins</strong>
                      </span>
                    ) : (
                      <span className="pending">Not played</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )
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