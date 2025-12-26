// frontend/src/components/RoofVisualizationMap.tsx

import React, { useMemo } from 'react';
import { GoogleMap, Polygon, OverlayView, useLoadScript } from '@react-google-maps/api';
import { calculateEdgeCompassLabels } from '../utils/mapUtils';
import { calculatePanelPositions, calculatePolygonCentroid } from '../utils/panelLayoutUtils';

const libraries: ('places' | 'drawing' | 'geometry')[] = ['geometry'];

interface PanelLayout {
  rows: number;
  columns: number;
  spacing: number;
  azimuth: number;
}

interface RoofVisualizationMapProps {
  center: { lat: number; lng: number };
  roofPolygon: Array<{ lat: number; lng: number }>;
  panelLayout: PanelLayout;
  showCompassLabels?: boolean;
  showPanels?: boolean;
  height?: string;
  width?: string;
}

const RoofVisualizationMap: React.FC<RoofVisualizationMapProps> = ({
  center,
  roofPolygon,
  panelLayout,
  showCompassLabels = true,
  showPanels = true,
  height = '600px',
  width = '100%',
}) => {
  const { isLoaded, loadError } = useLoadScript({
    googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_API_KEY || '',
    libraries,
  });

  const mapOptions = useMemo(
    () => ({
      mapTypeId: 'satellite' as google.maps.MapTypeId,
      tilt: 0,
      streetViewControl: false,
      mapTypeControl: true,
      fullscreenControl: true,
      zoomControl: true,
    }),
    []
  );

  // Calculate compass labels for edges
  const edgeLabels = useMemo(() => {
    if (!showCompassLabels || roofPolygon.length < 3) return [];
    return calculateEdgeCompassLabels(roofPolygon);
  }, [roofPolygon, showCompassLabels]);

  // Calculate panel positions
  const panelRectangles = useMemo(() => {
    if (!showPanels || roofPolygon.length < 3) return [];

    // Use roof centroid as the center point for panel layout
    const roofCenter = calculatePolygonCentroid(roofPolygon);

    return calculatePanelPositions(
      roofCenter,
      panelLayout.rows,
      panelLayout.columns,
      panelLayout.spacing,
      panelLayout.azimuth
    );
  }, [roofPolygon, panelLayout, showPanels]);

  if (loadError) {
    return <div>Error loading map</div>;
  }

  if (!isLoaded) {
    return <div>Loading map...</div>;
  }

  return (
    <GoogleMap
      mapContainerStyle={{ height, width }}
      center={center}
      zoom={20}
      options={mapOptions}
    >
      {/* Render roof polygon */}
      {roofPolygon.length >= 3 && (
        <Polygon
          paths={roofPolygon}
          options={{
            fillColor: '#4CAF50',
            fillOpacity: 0.2,
            strokeColor: '#2E7D32',
            strokeWeight: 2,
            strokeOpacity: 0.8,
          }}
        />
      )}

      {/* Render compass labels on edges */}
      {showCompassLabels &&
        edgeLabels.map((edge, index) => (
          <OverlayView
            key={`edge-${index}`}
            position={edge.position}
            mapPaneName={OverlayView.OVERLAY_MOUSE_TARGET}
          >
            <div
              style={{
                background: 'white',
                border: '2px solid #1976d2',
                borderRadius: '4px',
                padding: '4px 8px',
                fontWeight: 'bold',
                fontSize: '14px',
                color: '#1976d2',
                transform: 'translate(-50%, -50%)',
                whiteSpace: 'nowrap',
                boxShadow: '0 2px 6px rgba(0,0,0,0.3)',
              }}
            >
              {edge.label}
            </div>
          </OverlayView>
        ))}

      {/* Render panel rectangles */}
      {showPanels &&
        panelRectangles.map((panel, index) => (
          <Polygon
            key={`panel-${index}`}
            paths={panel.corners}
            options={{
              fillColor: '#2196F3',
              fillOpacity: 0.5,
              strokeColor: '#1976D2',
              strokeWeight: 1,
              strokeOpacity: 0.8,
            }}
          />
        ))}
    </GoogleMap>
  );
};

export default RoofVisualizationMap;
