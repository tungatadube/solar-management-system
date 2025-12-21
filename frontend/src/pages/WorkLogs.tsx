// frontend/src/pages/WorkLogs.tsx

import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import {
  Work as WorkIcon,
  CalendarToday as CalendarIcon,
  FilterAlt as FilterIcon,
  ClearAll as ClearIcon,
} from '@mui/icons-material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { workLogApi, userApi } from '../services/api';
import { WorkLog, User } from '../types';

const WorkLogs: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

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

  const loadWorkLogs = async () => {
    if (!selectedUserId) return;

    setLoading(true);
    try {
      let response;
      if (startDate && endDate) {
        response = await workLogApi.getByDateRange(selectedUserId, startDate, endDate);
      } else {
        response = await workLogApi.getByUser(selectedUserId);
      }
      setWorkLogs(response.data);
    } catch (error) {
      console.error('Failed to load work logs:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedUserId) {
      loadWorkLogs();
    }
  }, [selectedUserId]);

  const handleFilter = () => {
    loadWorkLogs();
  };

  const handleClearFilters = () => {
    setStartDate('');
    setEndDate('');
    loadWorkLogs();
  };

  const workLogColumns: GridColDef[] = [
    {
      field: 'workDate',
      headerName: 'Date',
      width: 120,
    },
    {
      field: 'startTime',
      headerName: 'Start',
      width: 100,
    },
    {
      field: 'endTime',
      headerName: 'End',
      width: 100,
    },
    {
      field: 'jobAddress',
      headerName: 'Address',
      width: 200,
      flex: 1,
    },
    {
      field: 'workDescription',
      headerName: 'Description',
      width: 250,
      flex: 1,
    },
    {
      field: 'workType',
      headerName: 'Type',
      width: 150,
      renderCell: (params) => (
        <Chip
          label={params.value.replace(/_/g, ' ')}
          size="small"
          variant="outlined"
        />
      ),
    },
    {
      field: 'hoursWorked',
      headerName: 'Hours',
      width: 90,
      valueFormatter: (params) => params.value.toFixed(2),
    },
    {
      field: 'hourlyRate',
      headerName: 'Rate',
      width: 90,
      valueFormatter: (params) => `$${params.value.toFixed(2)}`,
    },
    {
      field: 'totalAmount',
      headerName: 'Amount',
      width: 110,
      valueFormatter: (params) => `$${params.value.toFixed(2)}`,
    },
    {
      field: 'invoiced',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value ? 'Invoiced' : 'Uninvoiced'}
          color={params.value ? 'success' : 'warning'}
          size="small"
        />
      ),
    },
  ];

  const totalHours = workLogs.reduce((sum, log) => sum + log.hoursWorked, 0);
  const totalAmount = workLogs.reduce((sum, log) => sum + log.totalAmount, 0);
  const uninvoicedAmount = workLogs
    .filter(log => !log.invoiced)
    .reduce((sum, log) => sum + log.totalAmount, 0);
  const invoicedAmount = workLogs
    .filter(log => log.invoiced)
    .reduce((sum, log) => sum + log.totalAmount, 0);

  return (
    <Box p={3}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <WorkIcon fontSize="large" color="primary" />
        <Typography variant="h4">
          Work Logs
        </Typography>
      </Box>

      {/* Filters */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom display="flex" alignItems="center" gap={1}>
          <FilterIcon />
          Filters
        </Typography>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={3}>
            <FormControl fullWidth>
              <InputLabel>Technician</InputLabel>
              <Select
                value={selectedUserId || ''}
                onChange={(e) => setSelectedUserId(e.target.value as number)}
                label="Technician"
              >
                {users.map((user) => (
                  <MenuItem key={user.id} value={user.id}>
                    {user.firstName} {user.lastName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField
              label="Start Date"
              type="date"
              fullWidth
              InputLabelProps={{ shrink: true }}
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} md={3}>
            <TextField
              label="End Date"
              type="date"
              fullWidth
              InputLabelProps={{ shrink: true }}
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
            />
          </Grid>
          <Grid item xs={12} md={3}>
            <Box display="flex" gap={1}>
              <Button
                variant="contained"
                startIcon={<CalendarIcon />}
                onClick={handleFilter}
                fullWidth
                disabled={!startDate || !endDate}
              >
                Apply Filter
              </Button>
              <Button
                variant="outlined"
                startIcon={<ClearIcon />}
                onClick={handleClearFilters}
              >
                Clear
              </Button>
            </Box>
          </Grid>
        </Grid>
      </Paper>

      {/* Summary Cards */}
      <Grid container spacing={3} mb={3}>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="subtitle2" color="textSecondary">
                Total Logs
              </Typography>
              <Typography variant="h4">{workLogs.length}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="subtitle2" color="textSecondary">
                Total Hours
              </Typography>
              <Typography variant="h4">{totalHours.toFixed(2)}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="subtitle2" color="textSecondary">
                Total Amount
              </Typography>
              <Typography variant="h4" color="primary">
                ${totalAmount.toFixed(2)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography variant="subtitle2" color="textSecondary">
                Uninvoiced
              </Typography>
              <Typography variant="h4" color="warning.main">
                ${uninvoicedAmount.toFixed(2)}
              </Typography>
              <Typography variant="caption" color="textSecondary">
                Invoiced: ${invoicedAmount.toFixed(2)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Work Logs Table */}
      <Paper>
        <DataGrid
          rows={workLogs}
          columns={workLogColumns}
          pageSizeOptions={[10, 25, 50, 100]}
          initialState={{
            pagination: { paginationModel: { pageSize: 25 } },
            sorting: {
              sortModel: [{ field: 'workDate', sort: 'desc' }],
            },
          }}
          autoHeight
          loading={loading}
          disableRowSelectionOnClick
        />
      </Paper>
    </Box>
  );
};

export default WorkLogs;
