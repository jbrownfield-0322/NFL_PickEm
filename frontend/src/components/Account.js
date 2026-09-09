import React, { useState, useEffect } from 'react';
import { useAuth } from '../AuthContext';
import { API_BASE } from '../utils/api';

function Account() {
  const { user, login } = useAuth();
  const [currentUsername, setCurrentUsername] = useState(user ? user.username : '');
  const [currentName, setCurrentName] = useState(user ? user.name : '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (user) {
      setCurrentUsername(user.username);
      setCurrentName(user.name || '');
    }
  }, [user]);

  const updateUserField = async (path, body, onSuccess) => {
    setMessage('');
    setSaving(true);
    try {
      const response = await fetch(`${API_BASE}/user/${user.id}/${path}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      const text = await response.text();
      let data = {};
      if (text) {
        try {
          data = JSON.parse(text);
        } catch {
          data = { message: text };
        }
      }

      if (response.ok) {
        onSuccess(data);
      } else {
        setMessage(`Update failed: ${data.message || response.statusText || response.status}`);
      }
    } catch (error) {
      setMessage(`Error: ${error.message}`);
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateUsername = async (e) => {
    e.preventDefault();
    if (!currentUsername) {
      setMessage('Username cannot be empty.');
      return;
    }
    await updateUserField('updateUsername', { username: currentUsername }, (data) => {
      setMessage('Username updated successfully!');
      login({ ...user, username: data.username || currentUsername, name: data.name ?? user.name });
    });
  };

  const handleUpdateName = async (e) => {
    e.preventDefault();
    if (!currentName) {
      setMessage('Name cannot be empty.');
      return;
    }
    await updateUserField('updateName', { name: currentName }, (data) => {
      setMessage('Name updated successfully!');
      login({ ...user, name: data.name || currentName, username: data.username || user.username });
    });
  };

  const handleUpdatePassword = async (e) => {
    e.preventDefault();
    e.stopPropagation();

    if (!newPassword || !confirmNewPassword) {
      setMessage('Please enter and confirm your new password.');
      return;
    }

    if (newPassword !== confirmNewPassword) {
      setMessage('New passwords do not match.');
      return;
    }

    await updateUserField('updatePassword', { newPassword }, () => {
      setMessage('Password updated successfully!');
      setNewPassword('');
      setConfirmNewPassword('');
    });
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
        <button type="submit" disabled={saving}>Update Name</button>
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
        <button type="submit" disabled={saving}>Update Email</button>
      </form>

      <h3>Update Password</h3>
      <form onSubmit={handleUpdatePassword} action="#" method="post">
        <div>
          <label htmlFor="newPassword">New Password:</label>
          <input
            type="password"
            id="newPassword"
            name="newPassword"
            autoComplete="new-password"
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
            name="confirmNewPassword"
            autoComplete="new-password"
            value={confirmNewPassword}
            onChange={(e) => setConfirmNewPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit" disabled={saving}>
          {saving ? 'Updating...' : 'Update Password'}
        </button>
      </form>
    </section>
  );
}

export default Account;
