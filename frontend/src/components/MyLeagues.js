import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../AuthContext';

function MyLeagues() {
  const [leagues, setLeagues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { user } = useAuth();

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    const fetchMyLeagues = async () => {
      if (!user) {
        setLoading(false);
        return;
      }

      try {
        console.log('Fetching leagues for user:', user.id);
        const response = await fetch(`${API_BASE}/leagues/user/${user.id}`);
        console.log('Leagues response status:', response.status);
        
        if (!response.ok) {
          const errorText = await response.text();
          console.error('Leagues response error:', errorText);
          throw new Error(`Failed to fetch leagues: HTTP ${response.status} - ${errorText}`);
        }
        
        const data = await response.json();
        console.log('Leagues data:', data);
        setLeagues(data);
      } catch (error) {
        console.error('MyLeagues fetch error:', error);
        setError(error);
      } finally {
        setLoading(false);
      }
    };

    fetchMyLeagues();
  }, [user, API_BASE]);

  if (loading) {
    return <div>Loading your leagues...</div>;
  }

  if (error) {
    return (
      <div className="main-content">
        <h2>My Leagues</h2>
        <div style={{ color: 'red', padding: '20px', border: '1px solid red', borderRadius: '5px', margin: '20px 0' }}>
          <h3>Error Loading Leagues</h3>
          <p><strong>Error:</strong> {error.message}</p>
          <p>Please check the browser console for more details.</p>
          <button onClick={() => window.location.reload()}>Retry</button>
        </div>
      </div>
    );
  }

  return (
    <div className="main-content">
      <h2>My Leagues</h2>
      {leagues.length === 0 ? (
        <p>You are not currently a member of any leagues. <Link to="/leagues/create">Create one</Link> or <Link to="/leagues/join">join one</Link>!</p>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>League Name</th>
                <th>Join Code</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {leagues.map(league => (
                <tr key={league.id}>
                  <td data-label="League Name">{league.name}</td>
                  <td data-label="Join Code">{league.joinCode}</td>
                  <td data-label="Action"><Link to={`/leagues/${league.id}`}>View Details</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default MyLeagues; 