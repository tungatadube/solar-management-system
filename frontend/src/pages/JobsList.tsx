// frontend/src/pages/JobsList.tsx

import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
import { jobApi } from '../services/api';
import { Job, JobStatus, JobType } from '../types';
import GoogleMapDisplay from '../components/GoogleMapDisplay';

const JobsList: React.FC = () => {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedJob, setSelectedJob] = useState<Job | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'map'>('list');

  useEffect(() => {
    loadJobs();
  }, []);

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

  const handleUpdateStatus = async (id: number, status: JobStatus) => {
    try {
      await jobApi.updateStatus(id, status);
      loadJobs();
    } catch (error) {
      console.error('Failed to update job status:', error);
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
      field: 'scheduledStartTime',
      headerName: 'Scheduled Start',
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
              
              <Box mt={2}>
                <FormControl fullWidth>
                  <InputLabel>Update Status</InputLabel>
                  <Select
                    value={selectedJob.status}
                    onChange={(e) => handleUpdateStatus(selectedJob.id, e.target.value as JobStatus)}
                  >
                    {Object.values(JobStatus).map((status) => (
                      <MenuItem key={status} value={status}>
                        {status}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Box>
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
