import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { StationaryLocation, AddressResult } from '../types';
import {
  StationaryState,
  createInitialState,
  detectStationary,
  isWorkingHours,
  getCurrentLocation,
  getAdaptivePollingInterval,
} from '../services/locationTracker';
import { geocodingApi } from '../services/api';

interface StationaryTrackingContextType {
  isEnabled: boolean;
  isTracking: boolean;
  dismissedToday: StationaryLocation[];
  currentStationaryData: {
    location: { lat: number; lng: number; address: string } | null;
    duration: number;
  } | null;
  showPrompt: boolean;
  enableTracking: () => void;
  disableTracking: () => void;
  dismissLocation: (location: StationaryLocation) => void;
  closePrompt: () => void;
}

const StationaryTrackingContext = createContext<StationaryTrackingContextType | undefined>(
  undefined
);

export const useStationaryTracking = () => {
  const context = useContext(StationaryTrackingContext);
  if (!context) {
    throw new Error('useStationaryTracking must be used within StationaryTrackingProvider');
  }
  return context;
};

interface Props {
  children: React.ReactNode;
}

const DISMISSED_LOCATIONS_KEY = 'dismissedStationaryLocations';

export const StationaryTrackingProvider: React.FC<Props> = ({ children }) => {
  const [isEnabled, setIsEnabled] = useState(true);
  const [isTracking, setIsTracking] = useState(false);
  const [dismissedToday, setDismissedToday] = useState<StationaryLocation[]>([]);
  const [showPrompt, setShowPrompt] = useState(false);
  const [currentStationaryData, setCurrentStationaryData] = useState<{
    location: { lat: number; lng: number; address: string } | null;
    duration: number;
  } | null>(null);

  const stationaryStateRef = useRef<StationaryState>(createInitialState());
  const trackingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const consecutiveStationaryChecksRef = useRef<number>(0);

  // Load dismissed locations from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem(DISMISSED_LOCATIONS_KEY);
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        // Filter out locations from previous days
        const today = new Date().setHours(0, 0, 0, 0);
        const todayDismissed = parsed.filter((loc: StationaryLocation) => {
          const dismissedDate = new Date(loc.dismissedAt!).setHours(0, 0, 0, 0);
          return dismissedDate === today;
        });
        setDismissedToday(todayDismissed);
      } catch (e) {
        console.error('Failed to parse dismissed locations:', e);
        localStorage.removeItem(DISMISSED_LOCATIONS_KEY);
      }
    }
  }, []);

  // Save dismissed locations to localStorage whenever they change
  useEffect(() => {
    localStorage.setItem(DISMISSED_LOCATIONS_KEY, JSON.stringify(dismissedToday));
  }, [dismissedToday]);

  // Reset dismissed locations at midnight
  useEffect(() => {
    const now = new Date();
    const tomorrow = new Date(now);
    tomorrow.setHours(24, 0, 0, 0);
    const msUntilMidnight = tomorrow.getTime() - now.getTime();

    const timer = setTimeout(() => {
      setDismissedToday([]);
      // Reschedule for next midnight (recursive)
      window.location.reload(); // Reload to restart the timer
    }, msUntilMidnight);

    return () => clearTimeout(timer);
  }, []);

  // Check if location was already dismissed today
  const isLocationDismissed = useCallback(
    (lat: number, lng: number): boolean => {
      return dismissedToday.some((loc) => {
        // Check if within 50 meters
        const distance = Math.sqrt(
          Math.pow(loc.lat - lat, 2) + Math.pow(loc.lng - lng, 2)
        ) * 111000; // Rough conversion to meters
        return distance < 50;
      });
    },
    [dismissedToday]
  );

  // Track location and detect stationary status
  const trackLocation = useCallback(async () => {
    if (!isWorkingHours()) {
      console.log('Outside working hours, pausing tracking');
      return;
    }

    try {
      const position = await getCurrentLocation(false); // Low accuracy for battery savings

      const { newState, shouldPrompt } = detectStationary(
        position,
        stationaryStateRef.current
      );

      stationaryStateRef.current = newState;

      if (shouldPrompt) {
        const lat = newState.anchorPoint!.lat;
        const lng = newState.anchorPoint!.lng;

        // Check if already dismissed today
        if (isLocationDismissed(lat, lng)) {
          console.log('Location already dismissed today, skipping prompt');
          return;
        }

        // Reverse geocode to get address
        try {
          const response = await geocodingApi.reverseGeocode(lat, lng);
          const addressData = response.data as AddressResult;

          const duration = Date.now() - newState.stationaryStartTime!;

          setCurrentStationaryData({
            location: {
              lat,
              lng,
              address: addressData.formattedAddress,
            },
            duration,
          });

          setShowPrompt(true);
          consecutiveStationaryChecksRef.current = 0; // Reset after prompting
        } catch (error) {
          console.error('Failed to reverse geocode:', error);
        }
      } else {
        // Track consecutive stationary checks for adaptive polling
        if (newState.isStationary && newState.anchorPoint) {
          consecutiveStationaryChecksRef.current++;
        } else {
          consecutiveStationaryChecksRef.current = 0;
        }
      }
    } catch (error) {
      console.error('Error tracking location:', error);
    }
  }, [isLocationDismissed]);

  // Start/stop tracking based on isEnabled
  useEffect(() => {
    if (isEnabled && isWorkingHours()) {
      setIsTracking(true);

      // Start immediate tracking
      trackLocation();

      // Set up adaptive polling interval
      const scheduleNextPoll = () => {
        const interval = getAdaptivePollingInterval(consecutiveStationaryChecksRef.current);
        trackingIntervalRef.current = setTimeout(() => {
          trackLocation();
          scheduleNextPoll();
        }, interval);
      };

      scheduleNextPoll();

      return () => {
        if (trackingIntervalRef.current) {
          clearTimeout(trackingIntervalRef.current);
        }
        setIsTracking(false);
      };
    } else {
      setIsTracking(false);
    }
  }, [isEnabled, trackLocation]);

  const enableTracking = useCallback(() => {
    setIsEnabled(true);
  }, []);

  const disableTracking = useCallback(() => {
    setIsEnabled(false);
    stationaryStateRef.current = createInitialState();
  }, []);

  const dismissLocation = useCallback((location: StationaryLocation) => {
    const dismissedLocation: StationaryLocation = {
      ...location,
      dismissedAt: Date.now(),
    };
    setDismissedToday((prev) => [...prev, dismissedLocation]);
    setShowPrompt(false);
    setCurrentStationaryData(null);
  }, []);

  const closePrompt = useCallback(() => {
    setShowPrompt(false);
    setCurrentStationaryData(null);
  }, []);

  const value: StationaryTrackingContextType = {
    isEnabled,
    isTracking,
    dismissedToday,
    currentStationaryData,
    showPrompt,
    enableTracking,
    disableTracking,
    dismissLocation,
    closePrompt,
  };

  return (
    <StationaryTrackingContext.Provider value={value}>
      {children}
    </StationaryTrackingContext.Provider>
  );
};
