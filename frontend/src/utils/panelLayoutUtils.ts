// frontend/src/utils/panelLayoutUtils.ts

export interface PanelRectangle {
  corners: Array<{ lat: number; lng: number }>;
}

const PANEL_WIDTH = 1.0; // meters
const PANEL_HEIGHT = 1.7; // meters

/**
 * Calculate geographic position from local offset in meters
 * @param lat Base latitude
 * @param lng Base longitude
 * @param dx Offset in meters (east positive)
 * @param dy Offset in meters (north positive)
 * @returns New lat/lng position
 */
function offsetLatLng(
  lat: number,
  lng: number,
  dx: number,
  dy: number
): { lat: number; lng: number } {
  // Convert meter offsets to lat/lng deltas
  // 1 degree latitude ≈ 111,320 meters
  const dLat = dy / 111320;
  // 1 degree longitude varies with latitude
  const dLng = dx / (111320 * Math.cos((lat * Math.PI) / 180));

  return {
    lat: lat + dLat,
    lng: lng + dLng,
  };
}

/**
 * Rotate a point around origin
 * @param x X coordinate
 * @param y Y coordinate
 * @param degrees Rotation angle in degrees (clockwise)
 * @returns Rotated coordinates
 */
function rotatePoint(
  x: number,
  y: number,
  degrees: number
): { x: number; y: number } {
  const radians = (degrees * Math.PI) / 180;
  return {
    x: x * Math.cos(radians) - y * Math.sin(radians),
    y: x * Math.sin(radians) + y * Math.cos(radians),
  };
}

/**
 * Calculate panel positions for a given layout
 * @param roofCenter Center point of the roof
 * @param rows Number of panel rows
 * @param columns Number of panel columns
 * @param spacing Spacing between panels in meters
 * @param azimuth Panel orientation in degrees (0=North, 90=East, 180=South, 270=West)
 * @returns Array of panel rectangles with corner coordinates
 */
export function calculatePanelPositions(
  roofCenter: { lat: number; lng: number },
  rows: number,
  columns: number,
  spacing: number,
  azimuth: number
): PanelRectangle[] {
  const panels: PanelRectangle[] = [];

  // Calculate total array dimensions
  const arrayWidth = columns * PANEL_WIDTH + (columns - 1) * spacing;
  const arrayHeight = rows * PANEL_HEIGHT + (rows - 1) * spacing;

  // Calculate starting position (top-left of array)
  const startOffsetX = -arrayWidth / 2;
  const startOffsetY = arrayHeight / 2;

  for (let row = 0; row < rows; row++) {
    for (let col = 0; col < columns; col++) {
      // Calculate panel center in local coordinates
      const localX =
        startOffsetX + col * (PANEL_WIDTH + spacing) + PANEL_WIDTH / 2;
      const localY =
        startOffsetY - row * (PANEL_HEIGHT + spacing) - PANEL_HEIGHT / 2;

      // Create panel rectangle corners (before rotation)
      const corners = [
        { x: localX - PANEL_WIDTH / 2, y: localY + PANEL_HEIGHT / 2 }, // Top-left
        { x: localX + PANEL_WIDTH / 2, y: localY + PANEL_HEIGHT / 2 }, // Top-right
        { x: localX + PANEL_WIDTH / 2, y: localY - PANEL_HEIGHT / 2 }, // Bottom-right
        { x: localX - PANEL_WIDTH / 2, y: localY - PANEL_HEIGHT / 2 }, // Bottom-left
      ];

      // Rotate corners by azimuth
      const rotatedCorners = corners.map((corner) =>
        rotatePoint(corner.x, corner.y, azimuth)
      );

      // Convert to lat/lng
      const geoCorners = rotatedCorners.map((corner) =>
        offsetLatLng(roofCenter.lat, roofCenter.lng, corner.x, corner.y)
      );

      panels.push({ corners: geoCorners });
    }
  }

  return panels;
}

/**
 * Calculate the centroid of a polygon
 * @param polygon Array of coordinates
 * @returns Centroid position
 */
export function calculatePolygonCentroid(
  polygon: Array<{ lat: number; lng: number }>
): { lat: number; lng: number } {
  if (polygon.length === 0) {
    return { lat: 0, lng: 0 };
  }

  let latSum = 0;
  let lngSum = 0;

  for (const point of polygon) {
    latSum += point.lat;
    lngSum += point.lng;
  }

  return {
    lat: latSum / polygon.length,
    lng: lngSum / polygon.length,
  };
}
