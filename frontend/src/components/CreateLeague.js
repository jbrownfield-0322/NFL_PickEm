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
      
      // Check if response has content before trying to parse JSON
      const contentType = response.headers.get('content-type');
      let data = {};
      
      if (contentType && contentType.includes('application/json')) {
        const text = await response.text();
        if (text) {
          data = JSON.parse(text);
        }
      }
      
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