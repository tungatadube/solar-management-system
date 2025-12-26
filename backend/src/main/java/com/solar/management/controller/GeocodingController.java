package com.solar.management.controller;

import com.solar.management.service.GeocodingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/geocoding")
@CrossOrigin(origins = "*")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    /**
     * Reverse geocode coordinates to address
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Address information
     */
    @PostMapping("/reverse")
    public ResponseEntity<?> reverseGeocode(
            @RequestParam Double latitude,
            @RequestParam Double longitude
    ) {
        try {
            // Validate coordinates
            if (latitude == null || longitude == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Latitude and longitude are required"));
            }

            if (latitude < -90 || latitude > 90) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Latitude must be between -90 and 90"));
            }

            if (longitude < -180 || longitude > 180) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Longitude must be between -180 and 180"));
            }

            Map<String, Object> result = geocodingService.reverseGeocode(latitude, longitude);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to reverse geocode: " + e.getMessage()));
        }
    }

    /**
     * Clean up expired cache entries (admin endpoint)
     * @return Number of deleted entries
     */
    @DeleteMapping("/cache/cleanup")
    public ResponseEntity<?> cleanupCache() {
        try {
            int deleted = geocodingService.cleanupExpiredCache();
            return ResponseEntity.ok(Map.of("deleted", deleted));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to cleanup cache: " + e.getMessage()));
        }
    }
}
