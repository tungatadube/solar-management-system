import React from 'react';
import { GoogleMap, Marker, Polyline, InfoWindow } from '@react-google-maps/api';

interface MapMarker {
  id: string | number;
  position: {
    lat: number;
    lng: number;
  };
  title?: string;
  info?: React.ReactNode;
}

interface MapPolyline {
  path: { lat: number; lng: number }[];
  color?: string;
  weight?: number;
}

interface GoogleMapDisplayProps {
  center?: { lat: number; lng: number };
  zoom?: number;
  markers?: MapMarker[];
  polylines?: MapPolyline[];
  onMarkerClick?: (markerId: string | number) => void;
  height?: string;
  width?: string;
}

const defaultCenter = { lat: -34.9285, lng: 138.6007 }; // Adelaide, SA

const GoogleMapDisplay: React.FC<GoogleMapDisplayProps> = ({
  center = defaultCenter,
  zoom = 10,
  markers = [],
  polylines = [],
  onMarkerClick,
  height = '500px',
  width = '100%',
}) => {
  const [selectedMarkerId, setSelectedMarkerId] = React.useState<string | number | null>(null);

  const handleMarkerClick = (markerId: string | number) => {
    setSelectedMarkerId(markerId);
    if (onMarkerClick) {
      onMarkerClick(markerId);
    }
  };

  return (
    <GoogleMap
      mapContainerStyle={{ height, width }}
      center={center}
      zoom={zoom}
      options={{
        streetViewControl: false,
        mapTypeControl: true,
        fullscreenControl: true,
      }}
    >
      {/* Render polylines */}
      {polylines.map((polyline, index) => (
        <Polyline
          key={`polyline-${index}`}
          path={polyline.path}
          options={{
            strokeColor: polyline.color || '#FF0000',
            strokeWeight: polyline.weight || 2,
          }}
        />
      ))}

      {/* Render markers */}
      {markers.map((marker) => (
        <React.Fragment key={marker.id}>
          <Marker
            position={marker.position}
            title={marker.title}
            onClick={() => handleMarkerClick(marker.id)}
          />
          {selectedMarkerId === marker.id && marker.info && (
            <InfoWindow
              position={marker.position}
              onCloseClick={() => setSelectedMarkerId(null)}
            >
              <div>{marker.info}</div>
            </InfoWindow>
          )}
        </React.Fragment>
      ))}
    </GoogleMap>
  );
};

export default GoogleMapDisplay;
