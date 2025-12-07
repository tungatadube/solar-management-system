// frontend/src/pages/JobsList.tsx

import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Button,
  Chip,
  Paper,
  Typography,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  TextField,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { jobApi, parameterApi } from '../services/api';
import { Job, JobStatus, JobType } from '../types';
import GoogleMapDisplay from '../components/GoogleMapDisplay';

const JobsList: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');
  const [hourlyRate, setHourlyRate] = useState<number>(35);

  useEffect(() => {
    loadJobs();
    loadHourlyRate();
  }, [location.pathname]);

  const loadJobs = async () => {
    try {
      setLoading(true);
      const response = await jobApi.getAll();
      setJobs(response.data);
    } catch (error) {
      console.error('Failed to load jobs:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadHourlyRate = async () => {
    try {
      const response = await parameterApi.getHourlyRate();
      setHourlyRate(response.data);
    } catch (error) {
      console.error('Failed to load hourly rate, using default:', error);
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this job?')) {
      try {
        await jobApi.delete(id);
        loadJobs();
      } catch (error) {
        console.error('Failed to delete job:', error);
      }
    }
  };

  const getStatusColor = (status: JobStatus): "default" | "primary" | "secondary" | "success" | "warning" | "info" | "error" => {
    switch (status) {
      case JobStatus.SCHEDULED: return 'info';
      case JobStatus.IN_PROGRESS: return 'primary';
      case JobStatus.COMPLETED: return 'success';
      case JobStatus.ON_HOLD: return 'warning';
      case JobStatus.CANCELLED: return 'error';
      default: return 'default';
    }
  };

  const calculateDuration = (job: Job): number | null => {
    if (job.startTime && job.endTime) {
      const start = new Date(job.startTime);
      const end = new Date(job.endTime);
      const durationMs = end.getTime() - start.getTime();
      return durationMs / (1000 * 60 * 60); // Convert to hours
    }
    return null;
  };

  const calculateEarnings = (job: Job): number | null => {
    const duration = calculateDuration(job);
    if (duration !== null) {
      return duration * hourlyRate;
    }
    return null;
  };

  const columns: GridColDef[] = [
    { field: 'jobNumber', headerName: 'Job Number', width: 150 },
    { field: 'clientName', headerName: 'Client', width: 150 },
    {
      field: 'location',
      headerName: 'Location',
      width: 200,
      valueGetter: (params) => params.row.location?.address || '',
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 130,
      renderCell: (params) => (
        <Chip
          label={params.value}
          color={getStatusColor(params.value as JobStatus)}
          size="small"
        />
      ),
    },
    {
      field: 'type',
      headerName: 'Type',
      width: 150,
    },
    {
      field: 'assignedTechnicians',
      headerName: 'Assigned Technicians',
      width: 200,
      valueGetter: (params) => {
        const techs = params.row.assignedTechnicians || [];
        return techs.map((tech: any) => `${tech.firstName} ${tech.lastName}`).join(', ');
      },
    },
    {
      field: 'startTime',
      headerName: 'Start Time',
      width: 180,
      valueFormatter: (params) =>
        params.value ? new Date(params.value).toLocaleString() : '',
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 150,
      sortable: false,
      renderCell: (params) => (
        <Box>
          <IconButton
            size="small"
            onClick={() => {
              setSelectedJob(params.row);
              setOpenDialog(true);
            }}
          >
            <ViewIcon />
          </IconButton>
          <IconButton
            size="small"
            onClick={() => navigate(`/jobs/edit/${params.row.id}`)}
          >
            <EditIcon />
          </IconButton>
          <IconButton size="small" onClick={() => handleDelete(params.row.id)}>
            <DeleteIcon />
          </IconButton>
        </Box>
      ),
    },
  ];

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Jobs</Typography>
        <Box>
          <Button
            variant={viewMode === 'list' ? 'contained' : 'outlined'}
            onClick={() => setViewMode('list')}
            sx={{ mr: 1 }}
          >
            List View
          </Button>
          <Button
            variant={viewMode === 'map' ? 'contained' : 'outlined'}
            onClick={() => setViewMode('map')}
            sx={{ mr: 2 }}
          >
            Map View
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => navigate('/jobs/create')}
          >
            New Job
          </Button>
        </Box>
      </Box>

      {viewMode === 'list' ? (
        <Paper>
          <DataGrid
            rows={jobs}
            columns={columns}
            loading={loading}
            pageSizeOptions={[10, 25, 50]}
            initialState={{
              pagination: { paginationModel: { pageSize: 10 } },
            }}
            autoHeight
          />
        </Paper>
      ) : (
        <Paper sx={{ height: 600, p: 2 }}>
          <GoogleMapDisplay
            center={{ lat: -34.9285, lng: 138.6007 }}
            zoom={10}
            markers={jobs.map((job) => ({
              id: job.id,
              position: {
                lat: job.location.latitude,
                lng: job.location.longitude,
              },
              title: job.jobNumber,
              info: (
                <Box>
                  <Typography variant="subtitle2">{job.jobNumber}</Typography>
                  <Typography variant="body2">{job.clientName}</Typography>
                  <Typography variant="caption">{job.location.address}</Typography>
                  <br />
                  <Chip
                    label={job.status}
                    color={getStatusColor(job.status)}
                    size="small"
                  />
                </Box>
              ),
            }))}
            height="100%"
          />
        </Paper>
      )}

      {/* Job Details Dialog */}
      <Dialog
        open={openDialog}
        onClose={() => setOpenDialog(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          {selectedJob ? `Job Details: ${selectedJob.jobNumber}` : 'Create New Job'}
        </DialogTitle>
        <DialogContent>
          {selectedJob ? (
            <Box>
              <Typography variant="h6" gutterBottom>
                Client Information
              </Typography>
              <Typography>Name: {selectedJob.clientName}</Typography>
              <Typography>Phone: {selectedJob.clientPhone || 'N/A'}</Typography>
              <Typography>Email: {selectedJob.clientEmail || 'N/A'}</Typography>
              
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Job Details
              </Typography>
              <Typography>Type: {selectedJob.type}</Typography>
              <Typography>Status: {selectedJob.status}</Typography>
              <Typography>System Size: {selectedJob.systemSize} kW</Typography>

              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Time & Earnings
              </Typography>
              {selectedJob.startTime && (
                <Typography>
                  Start Time: {new Date(selectedJob.startTime).toLocaleString()}
                </Typography>
              )}
              {selectedJob.endTime && (
                <Typography>
                  End Time: {new Date(selectedJob.endTime).toLocaleString()}
                </Typography>
              )}
              {calculateDuration(selectedJob) !== null && (
                <>
                  <Typography>
                    Duration: {calculateDuration(selectedJob)?.toFixed(2)} hours
                  </Typography>
                  <Typography>
                    Earnings: ${calculateEarnings(selectedJob)?.toFixed(2)} AUD
                    (@ ${hourlyRate}/hr)
                  </Typography>
                </>
              )}
              {!selectedJob.startTime && !selectedJob.endTime && (
                <Typography color="text.secondary">
                  Time tracking not available (job not started)
                </Typography>
              )}

              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Assigned Technicians
              </Typography>
              <Typography>
                {selectedJob.assignedTechnicians?.map(tech =>
                  `${tech.firstName} ${tech.lastName}`
                ).join(', ') || 'None'}
              </Typography>

              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Location
              </Typography>
              <Typography>{selectedJob.location.address}</Typography>
            </Box>
          ) : (
            <Typography>Job creation form would go here</Typography>
          )}
        </DialogContent>
      </Dialog>
    </Box>
  );
};

export default JobsList;
