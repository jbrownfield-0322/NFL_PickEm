import React, { useState } from 'react';

function JoinLeague() {
  const [joinCode, setJoinCode] = useState('');
  const [message, setMessage] = useState('');

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(`${API_BASE}/leagues/join`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ joinCode }),
      });
      const data = await response.json();
      if (response.ok) {
        setMessage(`Successfully joined league: ${data.name}`);
      } else {
        setMessage(`Error joining league: ${data.message || response.statusText}`);
      }
    } catch (error) {
      setMessage(`Error: ${error.message}`);
    }
  };

  return (
    <section className="form-container">
      <h2>Join League</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>Join Code:</label>
          <input
            type="text"
            value={joinCode}
            onChange={(e) => setJoinCode(e.target.value)}
            required
          />
        </div>
        <button type="submit">Join League</button>
      </form>
      {message && <p>{message}</p>}
    </section>
  );
}

export default JoinLeague; 