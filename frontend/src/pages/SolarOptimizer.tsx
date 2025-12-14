// frontend/src/pages/SolarOptimizer.tsx

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  Button,
  Alert,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
} from '@mui/material';
import {
  WbSunny,
  Bolt,
  Straighten,
  AttachMoney,
  Build,
  Schedule,
} from '@mui/icons-material';
import RoofMeasurement, { RoofMeasurementData } from '../components/RoofMeasurement';
import { solarOptimizerApi, SolarAnalysis, SolarAnalysisRequest } from '../services/api';

const SolarOptimizer: React.FC = () => {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();

  const [showMeasurement, setShowMeasurement] = useState(true);
  const [analysis, setAnalysis] = useState<SolarAnalysis | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  // Load existing analysis if jobId is provided
  useEffect(() => {
    if (jobId) {
      loadExistingAnalysis(parseInt(jobId));
    }
  }, [jobId]);

  const loadExistingAnalysis = async (id: number) => {
    try {
      setLoading(true);
      const response = await solarOptimizerApi.getByJobId(id);
      if (response.data) {
        setAnalysis(response.data);
        setShowMeasurement(false);
      }
    } catch (err) {
      console.log('No existing analysis found for this job');
    } finally {
      setLoading(false);
    }
  };

  const handleMeasurementComplete = async (data: RoofMeasurementData) => {
    try {
      setLoading(true);
      setError('');

      const request: SolarAnalysisRequest = {
        jobId: jobId ? parseInt(jobId) : undefined,
        address: data.address,
        latitude: data.latitude,
        longitude: data.longitude,
        roofArea: data.roofArea,
        targetCapacity: data.targetCapacity,
        roofType: data.roofType,
      };

      const response = await solarOptimizerApi.analyze(request);
      setAnalysis(response.data);
      setShowMeasurement(false);
    } catch (err: any) {
      setError('Failed to perform solar analysis: ' + (err.response?.data?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const handleRecalculate = () => {
    setShowMeasurement(true);
    setAnalysis(null);
  };

  const handleSaveToJob = () => {
    if (jobId) {
      navigate(`/jobs/${jobId}`);
    } else {
      navigate('/jobs');
    }
  };

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('en-AU', {
      style: 'currency',
      currency: 'AUD',
    }).format(value);
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (showMeasurement || !analysis) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          Solar Panel Optimizer
        </Typography>
        <Typography variant="body1" color="text.secondary" paragraph>
          Measure the roof area and calculate the optimal solar panel configuration
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        <RoofMeasurement
          onMeasurementComplete={handleMeasurementComplete}
          initialLatitude={analysis?.latitude}
          initialLongitude={analysis?.longitude}
        />
      </Box>
    );
  }

  const materials = analysis.materials;

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">
          Solar Analysis Results
        </Typography>
        <Box sx={{ display: 'flex', gap: 2 }}>
          <Button variant="outlined" onClick={handleRecalculate}>
            Recalculate
          </Button>
          {jobId && (
            <Button variant="contained" onClick={handleSaveToJob}>
              Back to Job
            </Button>
          )}
        </Box>
      </Box>

      {/* Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Bolt color="primary" sx={{ mr: 1 }} />
                <Typography variant="subtitle2" color="text.secondary">
                  System Capacity
                </Typography>
              </Box>
              <Typography variant="h4">{analysis.systemCapacity.toFixed(2)} kW</Typography>
              <Typography variant="body2" color="text.secondary">
                {analysis.numberOfPanels} panels × {analysis.panelWattage}W
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <WbSunny color="warning" sx={{ mr: 1 }} />
                <Typography variant="subtitle2" color="text.secondary">
                  Annual Production
                </Typography>
              </Box>
              <Typography variant="h4">{analysis.annualProduction.toFixed(0)} kWh</Typography>
              <Typography variant="body2" color="text.secondary">
                {analysis.dailyAverage.toFixed(1)} kWh/day avg
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <Straighten color="info" sx={{ mr: 1 }} />
                <Typography variant="subtitle2" color="text.secondary">
                  Roof Coverage
                </Typography>
              </Box>
              <Typography variant="h4">{analysis.usableArea.toFixed(1)} m²</Typography>
              <Typography variant="body2" color="text.secondary">
                {((analysis.usableArea / analysis.roofArea) * 100).toFixed(0)}% of {analysis.roofArea.toFixed(1)} m²
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                <AttachMoney color="success" sx={{ mr: 1 }} />
                <Typography variant="subtitle2" color="text.secondary">
                  Estimated Cost
                </Typography>
              </Box>
              <Typography variant="h4">
                {materials ? formatCurrency(materials.totalCost) : 'N/A'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {materials ? `${formatCurrency(materials.totalCost / analysis.systemCapacity)}/kW` : ''}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Technical Details */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              System Configuration
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Grid container spacing={2}>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Optimal Azimuth
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {analysis.optimalAzimuth.toFixed(1)}°
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Optimal Tilt
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {analysis.optimalTilt.toFixed(1)}°
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Roof Orientation
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {analysis.roofOrientation}
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Roof Pitch
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {analysis.roofPitch.toFixed(1)}°
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Shading Factor
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {(analysis.shadingFactor * 100).toFixed(0)}%
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Peak Sun Hours
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {analysis.peakSunHours.toFixed(1)} hrs/day
                </Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        <Grid item xs={12} md={6}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Panel Layout
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Grid container spacing={2}>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Layout Configuration
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {analysis.layoutRows} rows × {analysis.layoutColumns} columns
                </Typography>
              </Grid>
              <Grid item xs={6}>
                <Typography variant="body2" color="text.secondary">
                  Panel Spacing
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {analysis.panelSpacing.toFixed(2)} m
                </Typography>
              </Grid>
              <Grid item xs={12}>
                <Typography variant="body2" color="text.secondary">
                  Location
                </Typography>
                <Typography variant="body1" fontWeight="bold">
                  {analysis.address || `${analysis.latitude.toFixed(6)}°, ${analysis.longitude.toFixed(6)}°`}
                </Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>

        {/* Materials Required */}
        {materials && (
          <Grid item xs={12}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Materials Required
              </Typography>
              <Divider sx={{ mb: 2 }} />

              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Category</TableCell>
                      <TableCell>Item</TableCell>
                      <TableCell align="right">Quantity</TableCell>
                      <TableCell align="right">Cost</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {/* Solar Panels */}
                    <TableRow>
                      <TableCell>Solar Panels</TableCell>
                      <TableCell>
                        {materials.panelType || 'Standard Solar Panel'} ({materials.panelDimensions || 'N/A'})
                      </TableCell>
                      <TableCell align="right">{materials.panelQuantity}</TableCell>
                      <TableCell align="right">{formatCurrency(materials.panelCost)}</TableCell>
                    </TableRow>

                    {/* Inverter */}
                    <TableRow>
                      <TableCell>Inverter</TableCell>
                      <TableCell>
                        {materials.inverterModel || materials.inverterType} ({materials.inverterCapacity?.toFixed(1)} kW)
                      </TableCell>
                      <TableCell align="right">{materials.inverterQuantity}</TableCell>
                      <TableCell align="right">{formatCurrency(materials.inverterCost)}</TableCell>
                    </TableRow>

                    {/* Mounting Hardware */}
                    <TableRow>
                      <TableCell>Mounting Rails</TableCell>
                      <TableCell>Aluminum rails</TableCell>
                      <TableCell align="right">{materials.railsQuantity} m</TableCell>
                      <TableCell align="right" rowSpan={4}>
                        {formatCurrency(materials.mountingCost)}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Mounting Clamps</TableCell>
                      <TableCell>Panel clamps</TableCell>
                      <TableCell align="right">{materials.clampsQuantity}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Roof Hooks</TableCell>
                      <TableCell>{materials.roofType || 'Standard'} roof hooks</TableCell>
                      <TableCell align="right">{materials.hooksQuantity}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Flashings</TableCell>
                      <TableCell>Weatherproof flashings</TableCell>
                      <TableCell align="right">{materials.flashingsQuantity}</TableCell>
                    </TableRow>

                    {/* Electrical */}
                    <TableRow>
                      <TableCell>DC Cable</TableCell>
                      <TableCell>Solar DC cable</TableCell>
                      <TableCell align="right">{materials.dcCableLength?.toFixed(1)} m</TableCell>
                      <TableCell align="right" rowSpan={6}>
                        {formatCurrency(materials.electricalCost)}
                      </TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>AC Cable</TableCell>
                      <TableCell>AC cable</TableCell>
                      <TableCell align="right">{materials.acCableLength?.toFixed(1)} m</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Conduit</TableCell>
                      <TableCell>Cable conduit</TableCell>
                      <TableCell align="right">{materials.conduitLength?.toFixed(1)} m</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>MC4 Connectors</TableCell>
                      <TableCell>MC4 connectors</TableCell>
                      <TableCell align="right">{materials.mcConnectors}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Isolator Switches</TableCell>
                      <TableCell>DC isolators</TableCell>
                      <TableCell align="right">{materials.isolatorQuantity}</TableCell>
                    </TableRow>
                    <TableRow>
                      <TableCell>Additional Components</TableCell>
                      <TableCell>Junction boxes, surge protectors, earthing kit</TableCell>
                      <TableCell align="right">
                        {materials.junctionBoxes + materials.surgeProtectors + materials.earthingKit}
                      </TableCell>
                    </TableRow>

                    {/* Labor */}
                    <TableRow>
                      <TableCell>Labor</TableCell>
                      <TableCell>
                        {materials.installationType} installation (~{materials.estimatedInstallDays} days)
                      </TableCell>
                      <TableCell align="right">-</TableCell>
                      <TableCell align="right">{formatCurrency(materials.laborCost)}</TableCell>
                    </TableRow>

                    {/* Total */}
                    <TableRow>
                      <TableCell colSpan={3} align="right">
                        <Typography variant="h6">Total Estimated Cost</Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Typography variant="h6">{formatCurrency(materials.totalCost)}</Typography>
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </Paper>
          </Grid>
        )}

        {/* Additional Info */}
        <Grid item xs={12}>
          <Alert severity="info">
            <Typography variant="subtitle2" gutterBottom>
              Important Notes:
            </Typography>
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              <li>These calculations are estimates based on standard conditions</li>
              <li>Actual production may vary based on weather, shading, and other factors</li>
              <li>Professional site inspection recommended before installation</li>
              <li>Costs are indicative and may vary based on supplier and market conditions</li>
              <li>Installation requires certified electrician and relevant permits</li>
            </ul>
          </Alert>
        </Grid>
      </Grid>
    </Box>
  );
};

export default SolarOptimizer;
