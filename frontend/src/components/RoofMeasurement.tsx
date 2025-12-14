// frontend/src/components/RoofMeasurement.tsx

import React, { useState, useCallback, useRef } from 'react';
import { GoogleMap, LoadScript, Polygon, DrawingManager } from '@react-google-maps/api';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  Alert,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { calculatePolygonArea } from '../utils/mapUtils';

interface RoofMeasurementProps {
  onMeasurementComplete: (data: RoofMeasurementData) => void;
  initialAddress?: string;
  initialLatitude?: number;
  initialLongitude?: number;
}

export interface RoofMeasurementData {
  address: string;
  latitude: number;
  longitude: number;
  roofArea: number;
  targetCapacity: number;
  roofType: string;
  coordinates: Array<{ lat: number; lng: number }>;
}

const libraries: ("drawing" | "geometry")[] = ["drawing", "geometry"];

const mapContainerStyle = {
  width: '100%',
  height: '500px',
};

const defaultCenter = {
  lat: -34.9285,
  lng: 138.6007,
};

const GOOGLE_MAPS_API_KEY = process.env.REACT_APP_GOOGLE_MAPS_API_KEY || '';

const RoofMeasurement: React.FC<RoofMeasurementProps> = ({
  onMeasurementComplete,
  initialAddress = '',
  initialLatitude,
  initialLongitude,
}) => {
  const [address, setAddress] = useState(initialAddress);
  const [center, setCenter] = useState(
    initialLatitude && initialLongitude
      ? { lat: initialLatitude, lng: initialLongitude }
      : defaultCenter
  );
  const [polygon, setPolygon] = useState<google.maps.Polygon | null>(null);
  const [roofArea, setRoofArea] = useState<number>(0);
  const [targetCapacity, setTargetCapacity] = useState<number>(5.0);
  const [roofType, setRoofType] = useState<string>('tile');
  const [error, setError] = useState<string>('');

  const mapRef = useRef<google.maps.Map | null>(null);
  const geocoderRef = useRef<google.maps.Geocoder | null>(null);

  const onMapLoad = useCallback((map: google.maps.Map) => {
    mapRef.current = map;
    geocoderRef.current = new google.maps.Geocoder();
  }, []);

  const handlePolygonComplete = useCallback((polygon: google.maps.Polygon) => {
    // Remove previous polygon if exists
    if (polygon) {
      polygon.setMap(null);
    }

    setPolygon(polygon);

    // Calculate area
    const path = polygon.getPath();
    const coordinates: Array<{ lat: number; lng: number }> = [];

    for (let i = 0; i < path.getLength(); i++) {
      const point = path.getAt(i);
      coordinates.push({ lat: point.lat(), lng: point.lng() });
    }

    const area = google.maps.geometry.spherical.computeArea(path);
    const areaInSquareMeters = Math.round(area * 100) / 100;

    setRoofArea(areaInSquareMeters);
    setError('');
  }, [polygon]);

  const handleAddressSearch = async () => {
    if (!address || !geocoderRef.current) {
      setError('Please enter an address');
      return;
    }

    try {
      const results = await new Promise<google.maps.GeocoderResult[]>((resolve, reject) => {
        geocoderRef.current!.geocode({ address }, (results, status) => {
          if (status === 'OK' && results) {
            resolve(results);
          } else {
            reject(new Error('Geocoding failed: ' + status));
          }
        });
      });

      if (results && results[0]) {
        const location = results[0].geometry.location;
        const newCenter = { lat: location.lat(), lng: location.lng() };
        setCenter(newCenter);

        if (mapRef.current) {
          mapRef.current.panTo(newCenter);
          mapRef.current.setZoom(20);
        }

        setError('');
      }
    } catch (err) {
      setError('Failed to find address. Please try again.');
      console.error('Geocoding error:', err);
    }
  };

  const handleClearPolygon = () => {
    if (polygon) {
      polygon.setMap(null);
      setPolygon(null);
    }
    setRoofArea(0);
  };

  const handleSubmit = () => {
    if (!polygon) {
      setError('Please draw a polygon on the map to measure the roof area');
      return;
    }

    if (roofArea === 0) {
      setError('Roof area must be greater than 0');
      return;
    }

    if (!targetCapacity || targetCapacity <= 0) {
      setError('Please enter a valid target capacity');
      return;
    }

    const path = polygon.getPath();
    const coordinates: Array<{ lat: number; lng: number }> = [];

    for (let i = 0; i < path.getLength(); i++) {
      const point = path.getAt(i);
      coordinates.push({ lat: point.lat(), lng: point.lng() });
    }

    onMeasurementComplete({
      address,
      latitude: center.lat,
      longitude: center.lng,
      roofArea,
      targetCapacity,
      roofType,
      coordinates,
    });
  };

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Roof Measurement
      </Typography>

      <Grid container spacing={3}>
        {/* Address Search */}
        <Grid item xs={12}>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <TextField
              fullWidth
              label="Property Address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleAddressSearch()}
              placeholder="Enter address to locate on map"
            />
            <Button variant="contained" onClick={handleAddressSearch}>
              Search
            </Button>
          </Box>
        </Grid>

        {/* Map */}
        <Grid item xs={12}>
          {GOOGLE_MAPS_API_KEY ? (
            <LoadScript
              googleMapsApiKey={GOOGLE_MAPS_API_KEY}
              libraries={libraries}
            >
              <GoogleMap
                mapContainerStyle={mapContainerStyle}
                center={center}
                zoom={20}
                mapTypeId="satellite"
                onLoad={onMapLoad}
                options={{
                  tilt: 0,
                  mapTypeControl: true,
                  streetViewControl: false,
                }}
              >
                <DrawingManager
                  onPolygonComplete={handlePolygonComplete}
                  options={{
                    drawingControl: true,
                    drawingControlOptions: {
                      position: google.maps.ControlPosition.TOP_CENTER,
                      drawingModes: [google.maps.drawing.OverlayType.POLYGON],
                    },
                    polygonOptions: {
                      fillColor: '#FF6B6B',
                      fillOpacity: 0.4,
                      strokeWeight: 2,
                      strokeColor: '#FF0000',
                      clickable: false,
                      editable: true,
                      zIndex: 1,
                    },
                  }}
                />
              </GoogleMap>
            </LoadScript>
          ) : (
            <Alert severity="warning">
              Google Maps API key not configured. Please set REACT_APP_GOOGLE_MAPS_API_KEY in your environment.
            </Alert>
          )}
        </Grid>

        {/* Measurement Results */}
        {roofArea > 0 && (
          <Grid item xs={12}>
            <Alert severity="success">
              Measured Roof Area: {roofArea.toFixed(2)} m²
            </Alert>
          </Grid>
        )}

        {/* Configuration */}
        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            label="Measured Roof Area (m²)"
            type="number"
            value={roofArea}
            onChange={(e) => setRoofArea(parseFloat(e.target.value) || 0)}
            InputProps={{
              readOnly: true,
            }}
          />
        </Grid>

        <Grid item xs={12} md={4}>
          <TextField
            fullWidth
            label="Target System Capacity (kW)"
            type="number"
            value={targetCapacity}
            onChange={(e) => setTargetCapacity(parseFloat(e.target.value) || 0)}
            inputProps={{ step: 0.5, min: 0 }}
          />
        </Grid>

        <Grid item xs={12} md={4}>
          <FormControl fullWidth>
            <InputLabel>Roof Type</InputLabel>
            <Select
              value={roofType}
              label="Roof Type"
              onChange={(e) => setRoofType(e.target.value)}
            >
              <MenuItem value="tile">Tile</MenuItem>
              <MenuItem value="metal">Metal</MenuItem>
              <MenuItem value="flat">Flat</MenuItem>
            </Select>
          </FormControl>
        </Grid>

        {/* Error Message */}
        {error && (
          <Grid item xs={12}>
            <Alert severity="error">{error}</Alert>
          </Grid>
        )}

        {/* Action Buttons */}
        <Grid item xs={12}>
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
            <Button variant="outlined" onClick={handleClearPolygon}>
              Clear Polygon
            </Button>
            <Button variant="contained" onClick={handleSubmit}>
              Calculate Solar System
            </Button>
          </Box>
        </Grid>

        {/* Instructions */}
        <Grid item xs={12}>
          <Alert severity="info">
            <Typography variant="subtitle2" gutterBottom>
              How to measure your roof:
            </Typography>
            <ol style={{ margin: 0, paddingLeft: 20 }}>
              <li>Enter the property address and click Search</li>
              <li>Use the satellite view to locate the roof</li>
              <li>Click the polygon tool in the map controls</li>
              <li>Click points around the roof perimeter to draw the outline</li>
              <li>Double-click to complete the polygon</li>
              <li>Enter the target system capacity and roof type</li>
              <li>Click "Calculate Solar System" to see the analysis</li>
            </ol>
          </Alert>
        </Grid>
      </Grid>
    </Paper>
  );
};

export default RoofMeasurement;
