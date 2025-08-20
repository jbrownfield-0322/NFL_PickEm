import React, { useState } from 'react';

function CreateLeague() {
  const [leagueName, setLeagueName] = useState('');
  const [message, setMessage] = useState('');
  const [joinCode, setJoinCode] = useState('');

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch(`${API_BASE}/leagues/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ leagueName }),
      });
      const data = await response.json();
      if (response.ok) {
        setMessage(`League "${data.name}" created successfully!`);
        setJoinCode(data.joinCode);
      } else {
        setMessage(`Error creating league: ${data.message || response.statusText}`);
        setJoinCode('');
      }
    } catch (error) {
      setMessage(`Error: ${error.message}`);
      setJoinCode('');
    }
  };

  return (
    <section className="form-container">
      <h2>Create New League</h2>
      <form onSubmit={handleSubmit}>
        <div>
          <label>League Name:</label>
          <input
            type="text"
            value={leagueName}
            onChange={(e) => setLeagueName(e.target.value)}
            required
          />
        </div>
        <button type="submit">Create League</button>
      </form>
      {message && <p>{message}</p>}
      {joinCode && <p>Share this join code with your friends: <strong>{joinCode}</strong></p>}
    </section>
  );
}

export default CreateLeague; 