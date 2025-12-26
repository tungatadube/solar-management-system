package com.solar.management.service;

import com.solar.management.entity.LocationTracking;
import com.solar.management.entity.User;
import com.solar.management.repository.LocationTrackingRepository;
import com.solar.management.repository.UserRepository;
import com.solar.management.security.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LocationTrackingService {

    private final LocationTrackingRepository locationTrackingRepository;
    private final UserRepository userRepository;
    private final AuthenticationHelper authHelper;

    public LocationTracking recordLocation(LocationTracking locationTracking) {
        log.info("Recording location for user: {} at ({}, {})", 
                 locationTracking.getUser().getUsername(),
                 locationTracking.getLatitude(),
                 locationTracking.getLongitude());
        
        return locationTrackingRepository.save(locationTracking);
    }
    
    public LocationTracking recordLocationByUserId(Long userId, Double latitude, Double longitude,
                                                   Double accuracy, Double altitude, Double speed,
                                                   Double heading, String deviceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        LocationTracking locationTracking = LocationTracking.builder()
                .user(user)
                .latitude(latitude)
                .longitude(longitude)
                .accuracy(accuracy)
                .altitude(altitude)
                .speed(speed)
                .heading(heading)
                .deviceId(deviceId)
                .timestamp(LocalDateTime.now())
                .build();
        
        return recordLocation(locationTracking);
    }
    
    public LocationTracking getLatestLocation(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return locationTrackingRepository.findLatestLocationForUser(user)
                .orElse(null);
    }
    
    public List<LocationTracking> getUserLocationHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return locationTrackingRepository.findByUserOrderByTimestampDesc(user);
    }
    
    public List<LocationTracking> getUserLocationHistoryBetweenDates(Long userId, 
                                                                     LocalDateTime start, 
                                                                     LocalDateTime end) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return locationTrackingRepository.findByUserAndTimestampBetween(user, start, end);
    }
    
    public Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // Haversine formula to calculate distance between two coordinates
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    }

    /**
     * Get latest location with access validation
     * Technicians can only access their own location data
     */
    public LocationTracking getLatestLocationWithAuth(Long userId) {
        authHelper.validateUserAccess(userId);
        return getLatestLocation(userId);
    }

    /**
     * Get user location history with access validation
     */
    public List<LocationTracking> getUserLocationHistoryWithAuth(Long userId) {
        authHelper.validateUserAccess(userId);
        return getUserLocationHistory(userId);
    }

    /**
     * Get location history between dates with access validation
     */
    public List<LocationTracking> getUserLocationHistoryBetweenDatesWithAuth(
            Long userId, LocalDateTime start, LocalDateTime end) {
        authHelper.validateUserAccess(userId);
        return getUserLocationHistoryBetweenDates(userId, start, end);
    }

    /**
     * Record location by user ID with access validation
     * Technicians can only record their own location
     */
    public LocationTracking recordLocationByUserIdWithAuth(
            Long userId, Double latitude, Double longitude,
            Double accuracy, Double altitude, Double speed,
            Double heading, String deviceId) {
        authHelper.validateUserAccess(userId);
        return recordLocationByUserId(userId, latitude, longitude,
                                      accuracy, altitude, speed, heading, deviceId);
    }
}
