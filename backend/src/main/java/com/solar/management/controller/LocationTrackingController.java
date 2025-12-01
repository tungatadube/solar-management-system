package com.solar.management.controller;

import com.solar.management.entity.LocationTracking;
import com.solar.management.service.LocationTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/location-tracking")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LocationTrackingController {
    
    private final LocationTrackingService locationTrackingService;
    
    @PostMapping
    public ResponseEntity<LocationTracking> recordLocation(@RequestBody LocationTracking locationTracking) {
        LocationTracking saved = locationTrackingService.recordLocation(locationTracking);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
    
    @PostMapping("/user/{userId}")
    public ResponseEntity<LocationTracking> recordLocationSimple(
            @PathVariable Long userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) Double accuracy,
            @RequestParam(required = false) Double altitude,
            @RequestParam(required = false) Double speed,
            @RequestParam(required = false) Double heading,
            @RequestParam(required = false) String deviceId) {
        
        LocationTracking saved = locationTrackingService.recordLocationByUserId(
                userId, latitude, longitude, accuracy, altitude, speed, heading, deviceId);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
    
    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<LocationTracking> getLatestLocation(@PathVariable Long userId) {
        LocationTracking latest = locationTrackingService.getLatestLocation(userId);
        if (latest == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(latest);
    }
    
    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<LocationTracking>> getUserLocationHistory(@PathVariable Long userId) {
        List<LocationTracking> history = locationTrackingService.getUserLocationHistory(userId);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/user/{userId}/history/range")
    public ResponseEntity<List<LocationTracking>> getUserLocationHistoryBetweenDates(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        
        List<LocationTracking> history = locationTrackingService.getUserLocationHistoryBetweenDates(userId, start, end);
        return ResponseEntity.ok(history);
    }
    
    @GetMapping("/distance")
    public ResponseEntity<Double> calculateDistance(
            @RequestParam Double lat1,
            @RequestParam Double lon1,
            @RequestParam Double lat2,
            @RequestParam Double lon2) {
        
        Double distance = locationTrackingService.calculateDistance(lat1, lon1, lat2, lon2);
        return ResponseEntity.ok(distance);
    }
}
