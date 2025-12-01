// frontend/src/pages/Dashboard.tsx

import React, { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Paper,
  Chip,
} from '@mui/material';
import {
  Work as WorkIcon,
  Schedule as ScheduleIcon,
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
} from '@mui/icons-material';
import { jobApi, stockApi } from '../services/api';
import { Job, JobStatus, StockItem } from '../types';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
);

interface DashboardStats {
  totalJobs: number;
  scheduledJobs: number;
  inProgressJobs: number;
  completedJobs: number;
  lowStockItems: number;
}

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<DashboardStats>({
    totalJobs: 0,
    scheduledJobs: 0,
    inProgressJobs: 0,
    completedJobs: 0,
    lowStockItems: 0,
  });
  const [recentJobs, setRecentJobs] = useState<Job[]>([]);
  const [lowStockItems, setLowStockItems] = useState<StockItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      // Load all jobs
      const jobsResponse = await jobApi.getAll();
      const allJobs = jobsResponse.data;
      
      // Calculate stats
      const scheduled = allJobs.filter(j => j.status === JobStatus.SCHEDULED).length;
      const inProgress = allJobs.filter(j => j.status === JobStatus.IN_PROGRESS).length;
      const completed = allJobs.filter(j => j.status === JobStatus.COMPLETED).length;
      
      // Load low stock items
      const lowStockResponse = await stockApi.getLowStock();
      const lowStock = lowStockResponse.data;
      
      setStats({
        totalJobs: allJobs.length,
        scheduledJobs: scheduled,
        inProgressJobs: inProgress,
        completedJobs: completed,
        lowStockItems: lowStock.length,
      });
      
      // Get recent jobs (last 5)
      setRecentJobs(allJobs.slice(0, 5));
      setLowStockItems(lowStock.slice(0, 5));
      
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const StatCard: React.FC<{
    title: string;
    value: number;
    icon: React.ReactNode;
    color: string;
  }> = ({ title, value, icon, color }) => (
    <Card>
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Box>
            <Typography color="textSecondary" gutterBottom variant="body2">
              {title}
            </Typography>
            <Typography variant="h4">{value}</Typography>
          </Box>
          <Box
            sx={{
              backgroundColor: color,
              borderRadius: '50%',
              width: 56,
              height: 56,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'white',
            }}
          >
            {icon}
          </Box>
        </Box>
      </CardContent>
    </Card>
  );

  const getStatusColor = (status: JobStatus): "default" | "primary" | "secondary" | "success" | "warning" | "info" | "error" => {
    switch (status) {
      case JobStatus.SCHEDULED:
        return 'info';
      case JobStatus.IN_PROGRESS:
        return 'primary';
      case JobStatus.COMPLETED:
        return 'success';
      case JobStatus.ON_HOLD:
        return 'warning';
      case JobStatus.CANCELLED:
        return 'error';
      default:
        return 'default';
    }
  };

  if (loading) {
    return <Typography>Loading dashboard...</Typography>;
  }

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>

      {/* Stats Cards */}
      <Grid container spacing={3} mb={3}>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Total Jobs"
            value={stats.totalJobs}
            icon={<WorkIcon />}
            color="#1976d2"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Scheduled"
            value={stats.scheduledJobs}
            icon={<ScheduleIcon />}
            color="#0288d1"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="In Progress"
            value={stats.inProgressJobs}
            icon={<WorkIcon />}
            color="#f57c00"
          />
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <StatCard
            title="Completed"
            value={stats.completedJobs}
            icon={<CheckCircleIcon />}
            color="#388e3c"
          />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        {/* Recent Jobs */}
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Recent Jobs
            </Typography>
            {recentJobs.map((job) => (
              <Box
                key={job.id}
                sx={{
                  p: 2,
                  mb: 1,
                  border: '1px solid #e0e0e0',
                  borderRadius: 1,
                }}
              >
                <Box display="flex" justifyContent="space-between" alignItems="center">
                  <Box>
                    <Typography variant="subtitle1" fontWeight="bold">
                      {job.jobNumber}
                    </Typography>
                    <Typography variant="body2" color="textSecondary">
                      {job.clientName} - {job.location.address}
                    </Typography>
                  </Box>
                  <Chip
                    label={job.status}
                    color={getStatusColor(job.status)}
                    size="small"
                  />
                </Box>
              </Box>
            ))}
          </Paper>
        </Grid>

        {/* Low Stock Alert */}
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Box display="flex" alignItems="center" mb={2}>
              <WarningIcon color="warning" sx={{ mr: 1 }} />
              <Typography variant="h6">Low Stock Items</Typography>
            </Box>
            {lowStockItems.length === 0 ? (
              <Typography variant="body2" color="textSecondary">
                All stock levels are good
              </Typography>
            ) : (
              lowStockItems.map((item) => (
                <Box
                  key={item.id}
                  sx={{
                    p: 1.5,
                    mb: 1,
                    backgroundColor: '#fff3e0',
                    borderRadius: 1,
                  }}
                >
                  <Typography variant="subtitle2">{item.name}</Typography>
                  <Typography variant="caption" color="textSecondary">
                    SKU: {item.sku}
                  </Typography>
                </Box>
              ))
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default Dashboard;
