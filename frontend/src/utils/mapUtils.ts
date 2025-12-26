// frontend/src/utils/mapUtils.ts

/**
 * Calculate the area of a polygon using the Shoelace formula
 * @param coordinates Array of latitude/longitude coordinates
 * @returns Area in square meters
 */
export function calculatePolygonArea(
  coordinates: Array<{ lat: number; lng: number }>
): number {
  if (coordinates.length < 3) {
    return 0;
  }

  // Convert lat/lng to meters using simple approximation
  // For more accurate results, use Google Maps Geometry library
  const R = 6371000; // Earth's radius in meters

  let area = 0;
  const numPoints = coordinates.length;

  for (let i = 0; i < numPoints; i++) {
    const j = (i + 1) % numPoints;
    const xi = coordinates[i].lng * (Math.PI / 180) * R * Math.cos(coordinates[i].lat * Math.PI / 180);
    const yi = coordinates[i].lat * (Math.PI / 180) * R;
    const xj = coordinates[j].lng * (Math.PI / 180) * R * Math.cos(coordinates[j].lat * Math.PI / 180);
    const yj = coordinates[j].lat * (Math.PI / 180) * R;

    area += xi * yj - xj * yi;
  }

  return Math.abs(area / 2);
}

/**
 * Calculate the distance between two points using the Haversine formula
 * @param lat1 Latitude of first point
 * @param lng1 Longitude of first point
 * @param lat2 Latitude of second point
 * @param lng2 Longitude of second point
 * @returns Distance in meters
 */
export function calculateDistance(
  lat1: number,
  lng1: number,
  lat2: number,
  lng2: number
): number {
  const R = 6371000; // Earth's radius in meters
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

/**
 * Format coordinates for display
 */
export function formatCoordinates(lat: number, lng: number): string {
  const latDir = lat >= 0 ? 'N' : 'S';
  const lngDir = lng >= 0 ? 'E' : 'W';

  return `${Math.abs(lat).toFixed(6)}° ${latDir}, ${Math.abs(lng).toFixed(6)}° ${lngDir}`;
}

/**
 * Convert degrees to radians
 */
function toRadians(degrees: number): number {
  return (degrees * Math.PI) / 180;
}

/**
 * Convert radians to degrees
 */
function toDegrees(radians: number): number {
  return (radians * 180) / Math.PI;
}

/**
 * Calculate bearing between two points (0-360 degrees, 0=North)
 * @param lat1 Latitude of first point
 * @param lng1 Longitude of first point
 * @param lat2 Latitude of second point
 * @param lng2 Longitude of second point
 * @returns Bearing in degrees (0-360)
 */
export function calculateBearing(
  lat1: number,
  lng1: number,
  lat2: number,
  lng2: number
): number {
  const dLon = toRadians(lng2 - lng1);
  const y = Math.sin(dLon) * Math.cos(toRadians(lat2));
  const x =
    Math.cos(toRadians(lat1)) * Math.sin(toRadians(lat2)) -
    Math.sin(toRadians(lat1)) * Math.cos(toRadians(lat2)) * Math.cos(dLon);
  const bearing = toDegrees(Math.atan2(y, x));
  return (bearing + 360) % 360; // Normalize to 0-360
}

/**
 * Convert bearing to compass label (N, NE, E, SE, S, SW, W, NW)
 * @param bearing Bearing in degrees (0-360)
 * @returns Compass label
 */
function getCompassLabel(bearing: number): string {
  const labels = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
  const index = Math.round(bearing / 45) % 8;
  return labels[index];
}

export interface EdgeLabel {
  position: { lat: number; lng: number };
  bearing: number;
  label: string;
}

/**
 * Calculate compass labels for each edge of a polygon
 * @param polygon Array of coordinates defining the polygon
 * @returns Array of edge labels with position and compass direction
 */
export function calculateEdgeCompassLabels(
  polygon: Array<{ lat: number; lng: number }>
): EdgeLabel[] {
  if (polygon.length < 3) {
    return [];
  }

  const labels: EdgeLabel[] = [];

  for (let i = 0; i < polygon.length; i++) {
    const p1 = polygon[i];
    const p2 = polygon[(i + 1) % polygon.length];

    // Calculate midpoint
    const midpoint = {
      lat: (p1.lat + p2.lat) / 2,
      lng: (p1.lng + p2.lng) / 2,
    };

    // Calculate bearing from p1 to p2
    const bearing = calculateBearing(p1.lat, p1.lng, p2.lat, p2.lng);

    // Convert bearing to compass label
    const label = getCompassLabel(bearing);

    labels.push({ position: midpoint, bearing, label });
  }

  return labels;
}
