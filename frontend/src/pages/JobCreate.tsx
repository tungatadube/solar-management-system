// frontend/src/pages/JobCreate.tsx

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  MenuItem,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  Checkbox,
  ListItemText,
  SelectChangeEvent,
} from '@mui/material';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { jobApi, locationApi, userApi } from '../services/api';
import { JobType, User, UserRole } from '../types';
import GoogleMapsAutocomplete from '../components/GoogleMapsAutocomplete';
import { formatDateToLocalISO } from '../utils/dateUtils';

const JobCreate: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  // Form state
  const [formData, setFormData] = useState({
    clientName: '',
    clientPhone: '',
    clientEmail: '',
    type: JobType.NEW_INSTALLATION,
    description: '',
    startTime: null as Date | null,
    endTime: null as Date | null,
    estimatedCost: '',
    systemSize: '',
    notes: '',
    assignedTechnicianIds: [] as string[],
  });

  // Location data from Google Maps
  const [locationData, setLocationData] = useState<{
    address: string;
    city: string;
    state: string;
    postalCode: string;
    country: string;
    latitude: number;
    longitude: number;
  } | null>(null);

  const [locationError, setLocationError] = useState(false);

  // Data for dropdowns
  const [users, setUsers] = useState<User[]>([]);
  const [loadingData, setLoadingData] = useState(true);

  // Load users for dropdown
  useEffect(() => {
    const fetchData = async () => {
      try {
        const usersRes = await userApi.getAll();
        // Filter only technicians and managers for assignment
        setUsers(usersRes.data.filter(u =>
          u.role === UserRole.TECHNICIAN || u.role === UserRole.MANAGER
        ));
      } catch (err: any) {
        setError('Failed to load form data: ' + (err.response?.data?.message || err.message));
      } finally {
        setLoadingData(false);
      }
    };
    fetchData();
  }, []);

  const handleChange = (field: string) => (event: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: event.target.value,
    }));
  };

  const handleDateChange = (field: 'startTime' | 'endTime') => (date: Date | null) => {
    setFormData(prev => ({
      ...prev,
      [field]: date,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(false);
    setLocationError(false);

    try {
      // Validate required fields
      if (!formData.clientName) {
        throw new Error('Client name is required');
      }
      if (!locationData) {
        setLocationError(true);
        throw new Error('Location is required. Please search and select a location.');
      }
      if (formData.assignedTechnicianIds.length === 0) {
        throw new Error('At least one technician must be assigned');
      }
      if (!formData.startTime) {
        throw new Error('Start time is required');
      }
      if (!formData.endTime) {
        throw new Error('End time is required');
      }

      // Create location first with unique name (includes timestamp)
      const timestamp = new Date().toISOString().split('T')[0];
      const locationName = `${formData.clientName} - ${locationData.address} (${timestamp})`;
      const locationPayload = {
        name: locationName,
        type: 'JOB_SITE' as const,
        address: locationData.address,
        city: locationData.city,
        state: locationData.state,
        postalCode: locationData.postalCode,
        country: locationData.country,
        latitude: locationData.latitude,
        longitude: locationData.longitude,
        active: true,
      };

      let createdLocation;
      try {
        const locationResponse = await locationApi.create(locationPayload);
        createdLocation = locationResponse.data;
      } catch (err: any) {
        // If duplicate error, try to find the existing location by name
        if (err.response?.data?.message?.includes('duplicate key') ||
            err.response?.data?.message?.includes('already exists')) {
          const allLocations = await locationApi.getAll();
          const existingLocation = allLocations.data.find(loc => loc.name === locationName);
          if (existingLocation) {
            createdLocation = existingLocation;
          } else {
            throw err;
          }
        } else {
          throw err;
        }
      }

      // Prepare job data
      const jobData: any = {
        clientName: formData.clientName,
        clientPhone: formData.clientPhone || undefined,
        clientEmail: formData.clientEmail || undefined,
        type: formData.type,
        description: formData.description || undefined,
        startTime: formatDateToLocalISO(formData.startTime),
        endTime: formatDateToLocalISO(formData.endTime),
        estimatedCost: formData.estimatedCost ? parseFloat(formData.estimatedCost) : undefined,
        systemSize: formData.systemSize ? parseInt(formData.systemSize) : undefined,
        notes: formData.notes || undefined,
        location: { id: createdLocation.id },
        assignedTechnicians: formData.assignedTechnicianIds.map(id => ({ id: parseInt(id) })),
      };

      await jobApi.create(jobData);
      setSuccess(true);

      // Navigate to jobs list after 1.5 seconds
      setTimeout(() => {
        navigate('/jobs');
      }, 1500);
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to create job');
    } finally {
      setLoading(false);
    }
  };

  if (loadingData) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          Create New Job
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {success && (
          <Alert severity="success" sx={{ mb: 3 }}>
            Job created successfully! Redirecting...
          </Alert>
        )}

        <Paper sx={{ p: 3 }}>
          <form onSubmit={handleSubmit}>
            <Grid container spacing={3}>
              {/* Client Information */}
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom>
                  Client Information
                </Typography>
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  required
                  fullWidth
                  label="Client Name"
                  value={formData.clientName}
                  onChange={handleChange('clientName')}
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  label="Client Phone"
                  value={formData.clientPhone}
                  onChange={handleChange('clientPhone')}
                  placeholder="+61-400-000-000"
                />
              </Grid>

              <Grid item xs={12} md={4}>
                <TextField
                  fullWidth
                  type="email"
                  label="Client Email"
                  value={formData.clientEmail}
                  onChange={handleChange('clientEmail')}
                  placeholder="client@example.com"
                />
              </Grid>

              {/* Job Details */}
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                  Job Details
                </Typography>
              </Grid>

              <Grid item xs={12} md={6}>
                <FormControl fullWidth required>
                  <InputLabel>Job Type</InputLabel>
                  <Select
                    value={formData.type}
                    onChange={handleChange('type')}
                    label="Job Type"
                  >
                    <MenuItem value={JobType.NEW_INSTALLATION}>New Installation</MenuItem>
                    <MenuItem value={JobType.MAINTENANCE}>Maintenance</MenuItem>
                    <MenuItem value={JobType.REPAIR}>Repair</MenuItem>
                    <MenuItem value={JobType.INSPECTION}>Inspection</MenuItem>
                    <MenuItem value={JobType.UPGRADE}>Upgrade</MenuItem>
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="System Size (kW)"
                  value={formData.systemSize}
                  onChange={handleChange('systemSize')}
                  inputProps={{ min: 0, step: 0.1 }}
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  multiline
                  rows={3}
                  label="Description"
                  value={formData.description}
                  onChange={handleChange('description')}
                  placeholder="Detailed description of the work to be done..."
                />
              </Grid>

              {/* Scheduling */}
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                  Scheduling
                </Typography>
              </Grid>

              <Grid item xs={12}>
                <GoogleMapsAutocomplete
                  onPlaceSelected={(place) => {
                    setLocationData(place);
                    setLocationError(false);
                  }}
                  label="Job Location"
                  required
                  error={locationError}
                  helperText={locationError ? "Please select a location from the dropdown" : "Start typing to search for an address"}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <FormControl fullWidth required>
                  <InputLabel>Assign Technicians</InputLabel>
                  <Select
                    multiple
                    value={formData.assignedTechnicianIds}
                    onChange={(e: SelectChangeEvent<string[]>) => {
                      setFormData(prev => ({
                        ...prev,
                        assignedTechnicianIds: e.target.value as string[],
                      }));
                    }}
                    label="Assign Technicians"
                    renderValue={(selected) => {
                      return users
                        .filter(u => selected.includes(u.id.toString()))
                        .map(u => `${u.firstName} ${u.lastName}`)
                        .join(', ');
                    }}
                  >
                    {users.map(user => (
                      <MenuItem key={user.id} value={user.id.toString()}>
                        <Checkbox checked={formData.assignedTechnicianIds.includes(user.id.toString())} />
                        <ListItemText primary={`${user.firstName} ${user.lastName} (${user.role})`} />
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>

              <Grid item xs={12} md={6}>
                <DateTimePicker
                  label="Start Time"
                  value={formData.startTime}
                  onChange={handleDateChange('startTime')}
                  slotProps={{
                    textField: {
                      fullWidth: true,
                      required: true,
                      helperText: "Required - Job start time",
                    },
                  }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <DateTimePicker
                  label="End Time"
                  value={formData.endTime}
                  onChange={handleDateChange('endTime')}
                  slotProps={{
                    textField: {
                      fullWidth: true,
                      required: true,
                      helperText: "Required - Job end time",
                    },
                  }}
                />
              </Grid>

              {/* Financial */}
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                  Financial
                </Typography>
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  type="number"
                  label="Estimated Cost ($)"
                  value={formData.estimatedCost}
                  onChange={handleChange('estimatedCost')}
                  inputProps={{ min: 0, step: 0.01 }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  multiline
                  rows={2}
                  label="Notes"
                  value={formData.notes}
                  onChange={handleChange('notes')}
                  placeholder="Any additional notes or special instructions..."
                />
              </Grid>

              {/* Action Buttons */}
              <Grid item xs={12}>
                <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', mt: 2 }}>
                  <Button
                    variant="outlined"
                    onClick={() => navigate('/jobs')}
                    disabled={loading}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="submit"
                    variant="contained"
                    disabled={loading}
                    startIcon={loading && <CircularProgress size={20} />}
                  >
                    {loading ? 'Creating...' : 'Create Job'}
                  </Button>
                </Box>
              </Grid>
            </Grid>
          </form>
        </Paper>
      </Box>
    </LocalizationProvider>
  );
};

export default JobCreate;
