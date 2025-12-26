// frontend/src/pages/LocationTracking.tsx

import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Typography,
} from '@mui/material';
import { MyLocation as MyLocationIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { locationTrackingApi, userApi } from '../services/api';
import { LocationTracking, User } from '../types';
import GoogleMapDisplay from '../components/GoogleMapDisplay';
import { useLoadScript } from '@react-google-maps/api';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';

const libraries: ("places" | "drawing" | "geometry")[] = ["places"];

const LocationTrackingPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [currentLocation, setCurrentLocation] = useState<LocationTracking | null>(null);
  const [locationHistory, setLocationHistory] = useState<LocationTracking[]>([]);
  const [loading, setLoading] = useState(false);
  const [trackingActive, setTrackingActive] = useState(false);

  const { isLoaded, loadError } = useLoadScript({
    googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_API_KEY || '',
    libraries,
  });

  useEffect(() => {
    loadUsers();
  }, []);

  useEffect(() => {
    if (selectedUserId) {
      loadUserLocation();
      loadLocationHistory();
    }
  }, [selectedUserId]);

  const loadUsers = async () => {
    try {
      const response = await userApi.getAll();
      setUsers(response.data);
      if (response.data.length > 0) {
        setSelectedUserId(response.data[0].id);
      }
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  };

  const loadUserLocation = async () => {
    if (!selectedUserId) return;
    
    try {
      setLoading(true);
      const response = await locationTrackingApi.getLatest(selectedUserId);
      setCurrentLocation(response.data);
    } catch (error) {
      console.error('Failed to load location:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadLocationHistory = async () => {
    if (!selectedUserId) return;
    
    try {
      const response = await locationTrackingApi.getHistory(selectedUserId);
      setLocationHistory(response.data);
    } catch (error) {
      console.error('Failed to load location history:', error);
    }
  };

  const startTracking = () => {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser');
      return;
    }

    setTrackingActive(true);
    
    // Get initial position
    navigator.geolocation.getCurrentPosition(
      (position) => {
        recordLocation(position.coords);
      },
      (error) => {
        console.error('Error getting location:', error);
        setTrackingActive(false);
      }
    );

    // Watch position changes
    const watchId = navigator.geolocation.watchPosition(
      (position) => {
        recordLocation(position.coords);
      },
      (error) => {
        console.error('Error watching location:', error);
      },
      {
        enableHighAccuracy: true,
        maximumAge: 10000,
        timeout: 5000,
      }
    );

    // Store watchId to stop tracking later
    (window as any).geoWatchId = watchId;
  };

  const stopTracking = () => {
    setTrackingActive(false);
    if ((window as any).geoWatchId) {
      navigator.geolocation.clearWatch((window as any).geoWatchId);
      (window as any).geoWatchId = null;
    }
  };

  const recordLocation = async (coords: GeolocationCoordinates) => {
    if (!selectedUserId) return;

    try {
      await locationTrackingApi.recordLocationSimple(selectedUserId, {
        latitude: coords.latitude,
        longitude: coords.longitude,
        accuracy: coords.accuracy,
        altitude: coords.altitude || undefined,
        speed: coords.speed || undefined,
        heading: coords.heading || undefined,
      });
      loadUserLocation();
      loadLocationHistory();
    } catch (error) {
      console.error('Failed to record location:', error);
    }
  };

  const pathCoordinates = locationHistory
    .map((loc) => ({ lat: loc.latitude, lng: loc.longitude }))
    .reverse();

  const mapCenter = currentLocation
    ? { lat: currentLocation.latitude, lng: currentLocation.longitude }
    : { lat: -34.9285, lng: 138.6007 }; // Adelaide default

  if (loadError) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">
          Error loading Google Maps. Please check your API key and try again.
        </Alert>
      </Box>
    );
  }

  if (!isLoaded) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Location Tracking
      </Typography>

      <Grid container spacing={3}>
        {/* Controls */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Tracking Controls
              </Typography>

              <FormControl fullWidth sx={{ mb: 2 }}>
                <InputLabel>Select User</InputLabel>
                <Select
                  value={selectedUserId || ''}
                  onChange={(e) => setSelectedUserId(e.target.value as number)}
                >
                  {users.map((user) => (
                    <MenuItem key={user.id} value={user.id}>
                      {user.firstName} {user.lastName}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Box display="flex" gap={1} mb={2}>
                <Button
                  variant="contained"
                  fullWidth
                  startIcon={<MyLocationIcon />}
                  onClick={trackingActive ? stopTracking : startTracking}
                  color={trackingActive ? 'error' : 'primary'}
                >
                  {trackingActive ? 'Stop Tracking' : 'Start Tracking'}
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => {
                    loadUserLocation();
                    loadLocationHistory();
                  }}
                >
                  <RefreshIcon />
                </Button>
              </Box>

              {currentLocation && (
                <Paper sx={{ p: 2, backgroundColor: '#f5f5f5' }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Current Location
                  </Typography>
                  <Typography variant="body2">
                    Lat: {currentLocation.latitude.toFixed(6)}
                  </Typography>
                  <Typography variant="body2">
                    Lng: {currentLocation.longitude.toFixed(6)}
                  </Typography>
                  {currentLocation.accuracy && (
                    <Typography variant="body2">
                      Accuracy: {currentLocation.accuracy.toFixed(0)}m
                    </Typography>
                  )}
                  {currentLocation.speed && (
                    <Typography variant="body2">
                      Speed: {(currentLocation.speed * 3.6).toFixed(1)} km/h
                    </Typography>
                  )}
                  <Typography variant="caption" color="textSecondary">
                    {new Date(currentLocation.timestamp).toLocaleString()}
                  </Typography>
                </Paper>
              )}
            </CardContent>
          </Card>

          {/* Location History */}
          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Locations ({locationHistory.length})
              </Typography>
              <Box sx={{ maxHeight: 300, overflowY: 'auto' }}>
                {locationHistory.slice(0, 10).map((loc) => (
                  <Paper key={loc.id} sx={{ p: 1, mb: 1, fontSize: '0.875rem' }}>
                    <Typography variant="caption" display="block">
                      {new Date(loc.timestamp).toLocaleString()}
                    </Typography>
                    <Typography variant="caption" color="textSecondary">
                      {loc.latitude.toFixed(4)}, {loc.longitude.toFixed(4)}
                    </Typography>
                  </Paper>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Map */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ height: 600, overflow: 'hidden' }}>
            <GoogleMapDisplay
              center={mapCenter}
              zoom={13}
              markers={currentLocation ? [{
                id: 'current',
                position: { lat: currentLocation.latitude, lng: currentLocation.longitude },
                title: 'Current Location',
                info: (
                  <Box>
                    <Typography variant="subtitle2">Current Location</Typography>
                    <Typography variant="caption">
                      {new Date(currentLocation.timestamp).toLocaleString()}
                    </Typography>
                    {currentLocation.speed && (
                      <Typography variant="caption" display="block">
                        Speed: {(currentLocation.speed * 3.6).toFixed(1)} km/h
                      </Typography>
                    )}
                  </Box>
                ),
              }] : []}
              polylines={pathCoordinates.length > 1 ? [{
                path: pathCoordinates,
                color: '#2196f3',
                weight: 3,
              }] : []}
              height="100%"
            />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default LocationTrackingPage;
