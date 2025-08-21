import React from 'react';
import { Routes, Route, Link, useNavigate } from 'react-router-dom'; // Remove BrowserRouter as Router
import { useAuth } from './AuthContext'; // Import useAuth
import './App.css';
import Register from './components/Register';
import Login from './components/Login';
import GameList from './components/GameList';
import Leaderboard from './components/Leaderboard';
import CreateLeague from './components/CreateLeague';
import JoinLeague from './components/JoinLeague';
import MyLeagues from './components/MyLeagues';
import LeagueDetails from './components/LeagueDetails';
import Account from './components/Account'; // Import Account component
import ProtectedRoute from './components/ProtectedRoute'; // Import ProtectedRoute
import GameManagement from './components/GameManagement'; // Import GameManagement component

function App() {
  const { isLoggedIn, logout } = useAuth(); // Use isLoggedIn and logout from AuthContext
  const navigate = useNavigate(); // Initialize navigate

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="App">
      <header className="App-header">
        <h1>NFL Pick'em App</h1>
        <nav>
          <ul className="main-nav">
            <li><Link to="/">Home</Link></li>
            {isLoggedIn && (
              <>
                <li><Link to="/games">Games</Link></li>
                <li><Link to="/leaderboard">Leaderboard</Link></li>
                <li><Link to="/leagues/create">Create League</Link></li>
                <li><Link to="/leagues/join">Join League</Link></li>
                <li><Link to="/my-leagues">My Leagues</Link></li>
                <li><Link to="/game-management">Game Management</Link></li>
              </>
            )}
            {isLoggedIn ? (
              <>
                <li><Link to="/account">Account</Link></li>
                <li><button onClick={handleLogout} className="nav-button">Logout</button></li>
              </>
            ) : (
              <>
                <li><Link to="/register">Register</Link></li>
                <li><Link to="/login">Login</Link></li>
              </>
            )}
          </ul>
        </nav>
      </header>
      <main className="main-content">
        <Routes>
          <Route path="/register" element={<Register />} />
          <Route path="/login" element={<Login />} />
          <Route path="/games" element={
            <ProtectedRoute>
              <GameList />
            </ProtectedRoute>
          } />
          <Route path="/leaderboard" element={
            <ProtectedRoute>
              <Leaderboard />
            </ProtectedRoute>
          } />
          <Route path="/leagues/create" element={
            <ProtectedRoute>
              <CreateLeague />
            </ProtectedRoute>
          } />
          <Route path="/leagues/join" element={
            <ProtectedRoute>
              <JoinLeague />
            </ProtectedRoute>
          } />
          <Route path="/my-leagues" element={
            <ProtectedRoute>
              <MyLeagues />
            </ProtectedRoute>
          } />
          <Route path="/leagues/:leagueId" element={
            <ProtectedRoute>
              <LeagueDetails />
            </ProtectedRoute>
          } />
          <Route path="/account" element={
            <ProtectedRoute>
              <Account />
            </ProtectedRoute>
          } />
          <Route path="/game-management" element={
            <ProtectedRoute>
              <GameManagement />
            </ProtectedRoute>
          } />
          <Route path="/" element={<h2>Welcome to the NFL Pick'em App!</h2>} />
        </Routes>
      </main>
    </div>
  );
}

export default App;
