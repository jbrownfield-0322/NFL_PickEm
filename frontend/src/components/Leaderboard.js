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