import { LocationPoint } from '../types';
import { calculateDistance } from '../utils/mapUtils';

const STATIONARY_THRESHOLD_METERS = 50;
const STATIONARY_DURATION_MS = 30 * 60 * 1000; // 30 minutes
const MIN_POINTS_FOR_DETECTION = 3;
const MAX_RECENT_POINTS = 10;
const WORKING_HOURS_START = 7.5; // 7:30 AM
const WORKING_HOURS_END = 18; // 6:00 PM
const SPEED_THRESHOLD_MPS = 1.4; // ~5 km/h in m/s

export interface StationaryState {
  isStationary: boolean;
  anchorPoint: LocationPoint | null;
  stationaryStartTime: number | null;
  recentPoints: LocationPoint[];
}

export function createInitialState(): StationaryState {
  return {
    isStationary: false,
    anchorPoint: null,
    stationaryStartTime: null,
    recentPoints: [],
  };
}

/**
 * Check if current time is within working hours (7:30 AM - 6:00 PM)
 */
export function isWorkingHours(): boolean {
  const now = new Date();
  const hour = now.getHours();
  const minute = now.getMinutes();
  const time = hour + minute / 60;
  return time >= WORKING_HOURS_START && time < WORKING_HOURS_END;
}

/**
 * Calculate centroid (average location) of recent points
 */
function getCentroid(points: LocationPoint[]): { lat: number; lng: number } {
  const sum = points.reduce(
    (acc, p) => ({ lat: acc.lat + p.lat, lng: acc.lng + p.lng }),
    { lat: 0, lng: 0 }
  );
  return {
    lat: sum.lat / points.length,
    lng: sum.lng / points.length,
  };
}

/**
 * Detect if user is stationary at a location
 * @param position Current geolocation position
 * @param state Current stationary state
 * @returns Updated state and whether to trigger prompt
 */
export function detectStationary(
  position: GeolocationPosition,
  state: StationaryState
): { newState: StationaryState; shouldPrompt: boolean } {
  const newPoint: LocationPoint = {
    lat: position.coords.latitude,
    lng: position.coords.longitude,
    timestamp: Date.now(),
    accuracy: position.coords.accuracy,
  };

  // Filter out points with low accuracy (>100m)
  if (newPoint.accuracy > 100) {
    return { newState: state, shouldPrompt: false };
  }

  // Check if user is moving based on speed
  const speed = position.coords.speed || 0;
  if (speed > SPEED_THRESHOLD_MPS) {
    // User is moving, reset stationary state
    return {
      newState: createInitialState(),
      shouldPrompt: false,
    };
  }

  // Add to recent points
  const updatedPoints = [...state.recentPoints, newPoint];
  if (updatedPoints.length > MAX_RECENT_POINTS) {
    updatedPoints.shift(); // Remove oldest
  }

  // Need enough data points
  if (updatedPoints.length < MIN_POINTS_FOR_DETECTION) {
    return {
      newState: { ...state, recentPoints: updatedPoints },
      shouldPrompt: false,
    };
  }

  // If no anchor point, set the first one
  if (!state.anchorPoint) {
    return {
      newState: {
        isStationary: true,
        anchorPoint: newPoint,
        stationaryStartTime: newPoint.timestamp,
        recentPoints: updatedPoints,
      },
      shouldPrompt: false,
    };
  }

  // Calculate distance from anchor point
  const distanceFromAnchor = calculateDistance(
    state.anchorPoint.lat,
    state.anchorPoint.lng,
    newPoint.lat,
    newPoint.lng
  );

  // Check if still within threshold
  if (distanceFromAnchor <= STATIONARY_THRESHOLD_METERS) {
    // Still stationary
    const stationaryDuration = newPoint.timestamp - state.stationaryStartTime!;

    if (stationaryDuration >= STATIONARY_DURATION_MS) {
      // Trigger prompt! Use centroid for more accurate location
      const centroid = getCentroid(updatedPoints);

      return {
        newState: {
          ...state,
          recentPoints: updatedPoints,
        },
        shouldPrompt: true,
      };
    } else {
      // Still accumulating time
      return {
        newState: {
          ...state,
          recentPoints: updatedPoints,
        },
        shouldPrompt: false,
      };
    }
  } else {
    // Moved outside threshold, reset with new anchor
    return {
      newState: {
        isStationary: true,
        anchorPoint: newPoint,
        stationaryStartTime: newPoint.timestamp,
        recentPoints: updatedPoints,
      },
      shouldPrompt: false,
    };
  }
}

/**
 * Get current location using browser geolocation API
 * @param highAccuracy Whether to use high accuracy mode
 * @returns Promise with geolocation position
 */
export function getCurrentLocation(highAccuracy: boolean = false): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('Geolocation is not supported by your browser'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      resolve,
      reject,
      {
        enableHighAccuracy: highAccuracy,
        timeout: 30000,
        maximumAge: highAccuracy ? 10000 : 120000,
      }
    );
  });
}

/**
 * Calculate adaptive polling interval based on stationary state
 * @param consecutiveStationaryChecks Number of consecutive stationary checks
 * @returns Polling interval in milliseconds
 */
export function getAdaptivePollingInterval(consecutiveStationaryChecks: number): number {
  const DEFAULT_INTERVAL = 5 * 60 * 1000; // 5 minutes
  const FREQUENT_INTERVAL = 2 * 60 * 1000; // 2 minutes
  const THRESHOLD = 2;

  return consecutiveStationaryChecks >= THRESHOLD ? FREQUENT_INTERVAL : DEFAULT_INTERVAL;
}
