import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';

function Account() {
  const { user, login } = useAuth(); // Assuming 'login' can also update user data
  const [currentUsername, setCurrentUsername] = useState(user ? user.username : '');
  const [currentName, setCurrentName] = useState(user ? user.name : '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [message, setMessage] = useState('');

  // API base URL - will work for both development and Railway production
  const API_BASE = process.env.REACT_APP_API_URL || (process.env.NODE_ENV === 'production' ? '/api' : 'http://localhost:8080');

  useEffect(() => {
    if (user) {
      setCurrentUsername(user.username);
      setCurrentName(user.name || '');
    }
  }, [user]);

  const handleUpdateUsername = async (e) => {
    e.preventDefault();
    setMessage('');

    if (!currentUsername) {
      setMessage('Username cannot be empty.');
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/user/${user.id}/updateUsername`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          // Add authorization header if your API requires it
          // 'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ username: currentUsername }),
      });

      const data = await response.json();
      if (response.ok) {
        setMessage('Username updated successfully!');
        login({ ...user, username: currentUsername }); // Update user in context
      } else {
        setMessage(`Failed to update username: ${data.message || response.statusText}`);
      }
    } catch (error) {
      setMessage(`Error: ${error.message}`);
    }
  };

  const handleUpdateName = async (e) => {
    e.preventDefault();
    setMessage('');

    if (!currentName) {
      setMessage('Name cannot be empty.');
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/user/${user.id}/updateName`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          // Add authorization header if your API requires it
          // 'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ name: currentName }),
      });

      const data = await response.json();
      if (response.ok) {
        setMessage('Name updated successfully!');
        login({ ...user, name: currentName }); // Update user in context
      } else {
        setMessage(`Failed to update name: ${data.message || response.statusText}`);
      }
    } catch (error) {
      setMessage(`Error: ${error.message}`);
    }
  };

  const handleUpdatePassword = async (e) => {
    e.preventDefault();
    setMessage('');

    if (!newPassword || !confirmNewPassword) {
      setMessage('Please enter and confirm your new password.');
      return;
    }

    if (newPassword !== confirmNewPassword) {
      setMessage('New passwords do not match.');
      return;
    }

    try {
      const response = await fetch(`${API_BASE}/user/${user.id}/updatePassword`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          // 'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ newPassword }),
      });

      const data = await response.json();
      if (response.ok) {
        setMessage('Password updated successfully!');
        setNewPassword('');
        setConfirmNewPassword('');
      } else {
        setMessage(`Failed to update password: ${data.message || response.statusText}`);
      }
    } catch (error) {
      setMessage(`Error: ${error.message}`);
    }
  };

  if (!user) {
    return <div className="main-content">Please log in to view your account details.</div>;
  }

  return (
    <section className="form-container">
      <h2>Account Information</h2>
      {message && <p className="message">{message}</p>}

      <div className="account-info">
        <h3>Current Information</h3>
        <p><strong>Email:</strong> {user.username}</p>
        <p><strong>Name:</strong> {user.name || 'Not set'}</p>
      </div>

      <h3>Update Name</h3>
      <form onSubmit={handleUpdateName}>
        <div>
          <label htmlFor="name">Display Name:</label>
          <input
            type="text"
            id="name"
            value={currentName}
            onChange={(e) => setCurrentName(e.target.value)}
            required
            placeholder="Enter your display name"
          />
        </div>
        <button type="submit">Update Name</button>
      </form>

      <h3>Update Username</h3>
      <form onSubmit={handleUpdateUsername}>
        <div>
          <label htmlFor="username">Email:</label>
          <input
            type="email"
            id="username"
            value={currentUsername}
            onChange={(e) => setCurrentUsername(e.target.value)}
            required
          />
        </div>
        <button type="submit">Update Email</button>
      </form>

      <h3>Update Password</h3>
      <form onSubmit={handleUpdatePassword}>
        <div>
          <label htmlFor="newPassword">New Password:</label>
          <input
            type="password"
            id="newPassword"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
          />
        </div>
        <div>
          <label htmlFor="confirmNewPassword">Confirm New Password:</label>
          <input
            type="password"
            id="confirmNewPassword"
            value={confirmNewPassword}
            onChange={(e) => setConfirmNewPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit">Update Password</button>
      </form>
    </section>
  );
}

export default Account;
