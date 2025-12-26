import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Checkbox,
  FormControlLabel,
} from '@mui/material';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import { useNavigate } from 'react-router-dom';
import { useStationaryTracking } from '../contexts/StationaryTrackingContext';

const StationaryJobPromptModal: React.FC = () => {
  const navigate = useNavigate();
  const { showPrompt, currentStationaryData, dismissLocation, closePrompt } =
    useStationaryTracking();

  const [dontAskAgain, setDontAskAgain] = useState(false);

  if (!currentStationaryData) {
    return null;
  }

  const { location, duration } = currentStationaryData;
  const durationMinutes = Math.floor(duration / (1000 * 60));

  const handleCreateJob = () => {
    if (!location) return;

    // Parse address to get components
    const addressParts = location.address.split(',').map((p) => p.trim());
    const [streetAddress, city, stateZip, country] = addressParts;
    const stateParts = stateZip?.split(' ') || [];
    const state = stateParts[0] || '';
    const postalCode = stateParts[1] || '';

    // Navigate to job creation with pre-filled location
    navigate('/jobs/create', {
      state: {
        locationData: {
          address: streetAddress || location.address,
          city: city || '',
          state: state,
          postalCode: postalCode,
          country: country || 'Australia',
          latitude: location.lat,
          longitude: location.lng,
        },
      },
    });

    // Dismiss if checkbox is checked
    if (dontAskAgain && location) {
      dismissLocation({
        lat: location.lat,
        lng: location.lng,
        address: location.address,
      });
    } else {
      closePrompt();
    }
  };

  const handleNotNow = () => {
    if (dontAskAgain && location) {
      dismissLocation({
        lat: location.lat,
        lng: location.lng,
        address: location.address,
      });
    } else {
      closePrompt();
    }
  };

  return (
    <Dialog open={showPrompt} onClose={handleNotNow} maxWidth="sm" fullWidth>
      <DialogTitle>
        <Box display="flex" alignItems="center" gap={1}>
          <LocationOnIcon color="primary" />
          <Typography variant="h6">Create Job for This Location?</Typography>
        </Box>
      </DialogTitle>

      <DialogContent>
        <Typography variant="body1" gutterBottom>
          You've been at this location for <strong>{durationMinutes}+ minutes</strong>. Would you
          like to create a job?
        </Typography>

        <Box mt={2} p={2} bgcolor="grey.100" borderRadius={1}>
          <Box display="flex" alignItems="start" gap={1}>
            <LocationOnIcon fontSize="small" color="action" sx={{ mt: 0.5 }} />
            <Typography variant="body2" color="text.secondary">
              {location?.address}
            </Typography>
          </Box>
        </Box>

        <Box mt={2}>
          <FormControlLabel
            control={
              <Checkbox
                checked={dontAskAgain}
                onChange={(e) => setDontAskAgain(e.target.checked)}
                size="small"
              />
            }
            label={
              <Typography variant="body2" color="text.secondary">
                Don't ask again today for this location
              </Typography>
            }
          />
        </Box>
      </DialogContent>

      <DialogActions>
        <Button onClick={handleNotNow} color="inherit">
          Not Now
        </Button>
        <Button onClick={handleCreateJob} variant="contained" color="primary">
          Create Job
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default StationaryJobPromptModal;
