// frontend/src/components/RailCutDisplay.tsx

import React, { useState } from 'react';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Grid,
  Chip,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';

export interface RailCut {
  length: number;
  count: number;
  source: '4m' | '6m';
  purpose: string;
}

interface RailCutDisplayProps {
  cuts: RailCut[];
  rails4m: number;
  rails6m: number;
  wastage: number;
}

const RailCutDisplay: React.FC<RailCutDisplayProps> = ({
  cuts,
  rails4m,
  rails6m,
  wastage,
}) => {
  const [railView, setRailView] = useState<'all' | '4m' | '6m'>('all');

  // Group cuts by source rail size
  const cuts4m = cuts.filter((cut) => cut.source === '4m');
  const cuts6m = cuts.filter((cut) => cut.source === '6m');

  const handleRailViewChange = (
    event: React.MouseEvent<HTMLElement>,
    newView: 'all' | '4m' | '6m' | null
  ) => {
    if (newView !== null) {
      setRailView(newView);
    }
  };

  return (
    <Box>
      {/* Toggle for Rail Size View */}
      <Box sx={{ display: 'flex', justifyContent: 'center', mb: 3 }}>
        <ToggleButtonGroup
          value={railView}
          exclusive
          onChange={handleRailViewChange}
          aria-label="rail size view"
          size="large"
        >
          <ToggleButton value="all" aria-label="show all rails">
            Show All Rails
          </ToggleButton>
          <ToggleButton value="4m" aria-label="show 4m rails">
            4m Rails Only
          </ToggleButton>
          <ToggleButton value="6m" aria-label="show 6m rails">
            6m Rails Only
          </ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {/* Summary Cards */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        {(railView === 'all' || railView === '4m') && (
          <Grid item xs={12} md={railView === 'all' ? 3 : 4}>
            <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#e3f2fd' }}>
              <Typography variant="body2" color="text.secondary">
                4m Rails Needed
              </Typography>
              <Typography variant="h4" color="primary">
                {rails4m}
              </Typography>
            </Paper>
          </Grid>
        )}
        {(railView === 'all' || railView === '6m') && (
          <Grid item xs={12} md={railView === 'all' ? 3 : 4}>
            <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#e8f5e9' }}>
              <Typography variant="body2" color="text.secondary">
                6m Rails Needed
              </Typography>
              <Typography variant="h4" color="success.main">
                {rails6m}
              </Typography>
            </Paper>
          </Grid>
        )}
        {railView === 'all' && (
          <Grid item xs={12} md={3}>
            <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#fff3e0' }}>
              <Typography variant="body2" color="text.secondary">
                Total Rails
              </Typography>
              <Typography variant="h4" color="warning.main">
                {rails4m + rails6m}
              </Typography>
            </Paper>
          </Grid>
        )}
        <Grid item xs={12} md={railView === 'all' ? 3 : 4}>
          <Paper sx={{ p: 2, textAlign: 'center', bgcolor: '#fce4ec' }}>
            <Typography variant="body2" color="text.secondary">
              Material Waste
            </Typography>
            <Typography variant="h4" color="error.main">
              {wastage.toFixed(2)}m
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      {/* Cutting Instructions */}
      <Grid container spacing={2}>
        {/* 4m Rails */}
        {cuts4m.length > 0 && (railView === 'all' || railView === '4m') && (
          <Grid item xs={12} md={railView === 'all' ? 6 : 12}>
            <TableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell colSpan={3} sx={{ bgcolor: '#1976d2', color: 'white' }}>
                      <Typography variant="h6">4m Rail Cuts</Typography>
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <strong>Length</strong>
                    </TableCell>
                    <TableCell align="center">
                      <strong>Quantity</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Purpose</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cuts4m.map((cut, index) => (
                    <TableRow key={index}>
                      <TableCell>
                        <Chip label={`${cut.length.toFixed(2)}m`} color="primary" size="small" />
                      </TableCell>
                      <TableCell align="center">{cut.count}</TableCell>
                      <TableCell>{cut.purpose}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Grid>
        )}

        {/* 6m Rails */}
        {cuts6m.length > 0 && (railView === 'all' || railView === '6m') && (
          <Grid item xs={12} md={railView === 'all' ? 6 : 12}>
            <TableContainer component={Paper}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell colSpan={3} sx={{ bgcolor: '#2e7d32', color: 'white' }}>
                      <Typography variant="h6">6m Rail Cuts</Typography>
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <strong>Length</strong>
                    </TableCell>
                    <TableCell align="center">
                      <strong>Quantity</strong>
                    </TableCell>
                    <TableCell>
                      <strong>Purpose</strong>
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cuts6m.map((cut, index) => (
                    <TableRow key={index}>
                      <TableCell>
                        <Chip label={`${cut.length.toFixed(2)}m`} color="success" size="small" />
                      </TableCell>
                      <TableCell align="center">{cut.count}</TableCell>
                      <TableCell>{cut.purpose}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Grid>
        )}
      </Grid>

      {/* No cuts available message */}
      {cuts.length === 0 && (
        <Paper sx={{ p: 3, textAlign: 'center' }}>
          <Typography color="text.secondary">
            No rail cutting plan available. Run a solar analysis to generate rail cuts.
          </Typography>
        </Paper>
      )}
    </Box>
  );
};

export default RailCutDisplay;
