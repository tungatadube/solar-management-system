// frontend/src/components/Sidebar.tsx

import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Box,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Divider,
  Avatar,
  Button,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  Work as WorkIcon,
  Inventory as InventoryIcon,
  MyLocation as LocationIcon,
  Schedule as ScheduleIcon,
  Assessment as ReportIcon,
  WbSunny as SolarIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material';
import { useAuth } from './AuthProvider';

const drawerWidth = 240;

const menuItems = [
  { text: 'Dashboard', icon: <DashboardIcon />, path: '/dashboard' },
  { text: 'Jobs', icon: <WorkIcon />, path: '/jobs' },
  { text: 'Stock Management', icon: <InventoryIcon />, path: '/stock' },
  { text: 'Location Tracking', icon: <LocationIcon />, path: '/tracking' },
  { text: 'Work Logs', icon: <ScheduleIcon />, path: '/worklogs' },
  { text: 'Solar Optimizer', icon: <SolarIcon />, path: '/solar-optimizer' },
  { text: 'Reports', icon: <ReportIcon />, path: '/reports' },
];

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        '& .MuiDrawer-paper': {
          width: drawerWidth,
          boxSizing: 'border-box',
          display: 'flex',
          flexDirection: 'column',
        },
      }}
    >
      <Toolbar>
        <Box display="flex" alignItems="center" gap={1}>
          <SolarIcon color="primary" />
          <Typography variant="h6" noWrap component="div">
            Solar Manager
          </Typography>
        </Box>
      </Toolbar>
      <Divider />
      <List>
        {menuItems.map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton
              selected={location.pathname === item.path}
              onClick={() => navigate(item.path)}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
      <Box sx={{ flexGrow: 1 }} />
      <Divider />
      <Box sx={{ p: 2 }}>
        <Box display="flex" alignItems="center" gap={1} mb={2}>
          <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
            {user?.firstName?.charAt(0) || 'U'}
          </Avatar>
          <Box>
            <Typography variant="body2" fontWeight="bold">
              {user?.firstName} {user?.lastName}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {user?.email}
            </Typography>
          </Box>
        </Box>
        <Button
          fullWidth
          variant="outlined"
          startIcon={<LogoutIcon />}
          onClick={logout}
          size="small"
        >
          Logout
        </Button>
      </Box>
    </Drawer>
  );
};

export default Sidebar;
