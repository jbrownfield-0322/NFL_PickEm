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
        const response = await fetch(`${API_BASE}/leagues/user/${user.id}`);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        setLeagues(data);
      } catch (error) {
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
    return <div>Error: {error.message}</div>;
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
                  <td>{league.name}</td>
                  <td>{league.joinCode}</td>
                  <td><Link to={`/leagues/${league.id}`}>View Details</Link></td>
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