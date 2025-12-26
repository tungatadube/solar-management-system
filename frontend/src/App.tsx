// frontend/src/App.tsx

import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Box, CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { AuthProvider } from './components/AuthProvider';
import { StationaryTrackingProvider } from './contexts/StationaryTrackingContext';
import StationaryJobPromptModal from './components/StationaryJobPromptModal';
import Sidebar from './components/Sidebar';
import Dashboard from './pages/Dashboard';
import JobsList from './pages/JobsList';
import JobCreate from './pages/JobCreate';
import JobEdit from './pages/JobEdit';
import StockManagement from './pages/StockManagement';
import LocationTracking from './pages/LocationTracking';
import WorkLogs from './pages/WorkLogs';
import Reports from './pages/Reports';
import SolarOptimizer from './pages/SolarOptimizer';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
  typography: {
    fontFamily: '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", "Helvetica Neue", Arial, sans-serif',
    fontSize: 15,
    fontWeightLight: 300,
    fontWeightRegular: 400,
    fontWeightMedium: 500,
    fontWeightBold: 600,
    h1: {
      fontSize: '2.5rem',
      fontWeight: 600,
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 600,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 500,
    },
    h6: {
      fontSize: '1.1rem',
      fontWeight: 500,
    },
    body1: {
      fontSize: '1rem',
      lineHeight: 1.6,
    },
    body2: {
      fontSize: '0.95rem',
      lineHeight: 1.5,
    },
    button: {
      textTransform: 'none',
      fontWeight: 500,
    },
  },
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          WebkitFontSmoothing: 'antialiased',
          MozOsxFontSmoothing: 'grayscale',
          fontSmooth: 'always',
          textRendering: 'optimizeLegibility',
        },
      },
    },
  },
});

const App: React.FC = () => {
  return (
    <AuthProvider>
      <StationaryTrackingProvider>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider theme={theme}>
            <CssBaseline />
            <Router>
              <Box sx={{ display: 'flex' }}>
                <Sidebar />
                <Box
                  component="main"
                  sx={{
                    flexGrow: 1,
                    bgcolor: 'background.default',
                    minHeight: '100vh',
                  }}
                >
                  <Routes>
                    <Route path="/" element={<Navigate to="/dashboard" replace />} />
                    <Route path="/dashboard" element={<Dashboard />} />
                    <Route path="/jobs" element={<JobsList />} />
                    <Route path="/jobs/create" element={<JobCreate />} />
                    <Route path="/jobs/edit/:id" element={<JobEdit />} />
                    <Route path="/stock" element={<StockManagement />} />
                    <Route path="/tracking" element={<LocationTracking />} />
                    <Route path="/worklogs" element={<WorkLogs />} />
                    <Route path="/reports" element={<Reports />} />
                    <Route path="/solar-optimizer" element={<SolarOptimizer />} />
                    <Route path="/solar-optimizer/job/:jobId" element={<SolarOptimizer />} />
                  </Routes>
                </Box>
              </Box>
              <StationaryJobPromptModal />
            </Router>
          </ThemeProvider>
        </QueryClientProvider>
      </StationaryTrackingProvider>
    </AuthProvider>
  );
};

export default App;
