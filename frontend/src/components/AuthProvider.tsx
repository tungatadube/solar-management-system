import React, { createContext, useContext, useEffect, useState } from 'react';
import { CircularProgress, Box, Typography } from '@mui/material';
import keycloak from '../keycloak';

interface AuthContextType {
  isAuthenticated: boolean;
  user: any;
  token: string | undefined;
  logout: () => void;
  login: () => void;
  hasRole: (role: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    const initKeycloak = async () => {
      try {
        const authenticated = await keycloak.init({
          onLoad: 'login-required',
          checkLoginIframe: false,
          pkceMethod: 'S256',
        });

        setIsAuthenticated(authenticated);

        if (authenticated) {
          // Get user info from token claims instead of making API call
          const tokenParsed = keycloak.tokenParsed;
          if (tokenParsed) {
            setUser({
              username: tokenParsed.preferred_username,
              email: tokenParsed.email,
              firstName: tokenParsed.given_name,
              lastName: tokenParsed.family_name,
              name: tokenParsed.name,
              roles: tokenParsed.realm_access?.roles || []
            });
          }

          // Setup token refresh
          setInterval(() => {
            keycloak.updateToken(70).catch(() => {
              console.error('Failed to refresh token');
              keycloak.logout();
            });
          }, 60000); // Refresh every minute
        }
      } catch (error) {
        console.error('Keycloak initialization failed:', error);
      } finally {
        setIsLoading(false);
      }
    };

    initKeycloak();
  }, []);

  const logout = () => {
    keycloak.logout({
      redirectUri: window.location.origin,
    });
  };

  const login = () => {
    keycloak.login();
  };

  const hasRole = (role: string): boolean => {
    return keycloak.hasRealmRole(role);
  };

  if (isLoading) {
    return (
      <Box
        display="flex"
        flexDirection="column"
        justifyContent="center"
        alignItems="center"
        minHeight="100vh"
        bgcolor="background.default"
      >
        <CircularProgress size={60} />
        <Typography variant="h6" sx={{ mt: 2 }}>
          Loading Solar Management System...
        </Typography>
      </Box>
    );
  }

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        user,
        token: keycloak.token,
        logout,
        login,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
