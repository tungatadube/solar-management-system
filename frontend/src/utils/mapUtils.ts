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
