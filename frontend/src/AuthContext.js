import React, { createContext, useState, useEffect, useContext } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // Check for stored token/user on initial load
  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    console.log('Stored user data:', storedUser); // Debug log
    
    if (storedUser) {
      try {
        const userData = JSON.parse(storedUser);
        console.log('Parsed user data:', userData); // Debug log
        
        // Validate that the user data has the expected structure
        if (userData && userData.id && userData.username) {
          console.log('Setting user as logged in:', userData); // Debug log
          setUser(userData);
          setIsLoggedIn(true);
        } else {
          console.log('Invalid user data structure, clearing:', userData); // Debug log
          logout(); // Clear invalid data
        }
      } catch (e) {
        console.error('Error parsing stored user data:', e); // Debug log
        logout(); // Clear invalid data
      }
    } else {
      console.log('No stored user data found'); // Debug log
    }
    
    // Mark loading as complete
    setIsLoading(false);
  }, []);

  const login = (userData) => {
    console.log('Login called with user data:', userData); // Debug log
    setIsLoggedIn(true);
    setUser(userData);
    localStorage.setItem('user', JSON.stringify(userData));
    console.log('User data saved to localStorage'); // Debug log
  };

  const logout = () => {
    setIsLoggedIn(false);
    setUser(null);
    localStorage.removeItem('user');
    // In a real application, you'd also invalidate the session/token on the backend
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, user, login, logout, isLoading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
