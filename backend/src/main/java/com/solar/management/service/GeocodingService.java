package com.solar.management.service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.LatLng;
import com.solar.management.entity.GeocodingCache;
import com.solar.management.repository.GeocodingCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);

    private final GeocodingCacheRepository cacheRepository;
    private final GeoApiContext geoApiContext;

    public GeocodingService(
            GeocodingCacheRepository cacheRepository,
            @Value("${google.maps.api-key}") String apiKey
    ) {
        this.cacheRepository = cacheRepository;
        this.geoApiContext = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Reverse geocode coordinates to address with caching
     * @param latitude Latitude
     * @param longitude Longitude
     * @return Map containing address components
     */
    @Transactional
    public Map<String, Object> reverseGeocode(double latitude, double longitude) {
        // Round coordinates to 5 decimal places (~1m precision) for caching
        double roundedLat = roundCoordinate(latitude);
        double roundedLng = roundCoordinate(longitude);

        // Check cache first
        Optional<GeocodingCache> cached = cacheRepository.findByLatitudeAndLongitude(roundedLat, roundedLng);

        if (cached.isPresent() && cached.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            logger.info("Cache hit for coordinates: {}, {}", roundedLat, roundedLng);
            return convertCacheToResponse(cached.get());
        }

        // Cache miss or expired - call Google API
        logger.info("Cache miss for coordinates: {}, {}, calling Google Maps API", roundedLat, roundedLng);

        try {
            GeocodingResult[] results = GeocodingApi.reverseGeocode(
                geoApiContext,
                new LatLng(latitude, longitude)
            ).await();

            if (results == null || results.length == 0) {
                throw new RuntimeException("No geocoding results found for coordinates: " + latitude + ", " + longitude);
            }

            GeocodingResult result = results[0];
            Map<String, String> components = extractAddressComponents(result.addressComponents);

            // Create cache entry
            GeocodingCache cacheEntry = new GeocodingCache(
                roundedLat,
                roundedLng,
                result.formattedAddress,
                components.get("city"),
                components.get("state"),
                components.get("postalCode"),
                components.get("country")
            );

            // Save to cache
            cacheRepository.save(cacheEntry);

            return convertCacheToResponse(cacheEntry);

        } catch (Exception e) {
            logger.error("Error during reverse geocoding for coordinates: {}, {}", latitude, longitude, e);
            throw new RuntimeException("Failed to reverse geocode coordinates: " + e.getMessage(), e);
        }
    }

    /**
     * Extract address components from Google API result
     */
    private Map<String, String> extractAddressComponents(AddressComponent[] components) {
        Map<String, String> result = new HashMap<>();

        for (AddressComponent component : components) {
            for (AddressComponentType type : component.types) {
                switch (type) {
                    case STREET_NUMBER:
                        result.put("streetNumber", component.longName);
                        break;
                    case ROUTE:
                        result.put("route", component.longName);
                        break;
                    case LOCALITY:
                        result.put("city", component.longName);
                        break;
                    case ADMINISTRATIVE_AREA_LEVEL_1:
                        result.put("state", component.shortName);
                        break;
                    case POSTAL_CODE:
                        result.put("postalCode", component.longName);
                        break;
                    case COUNTRY:
                        result.put("country", component.longName);
                        break;
                    default:
                        break;
                }
            }
        }

        return result;
    }

    /**
     * Convert cache entity to response map
     */
    private Map<String, Object> convertCacheToResponse(GeocodingCache cache) {
        Map<String, Object> response = new HashMap<>();
        response.put("formattedAddress", cache.getFormattedAddress());
        response.put("city", cache.getCity());
        response.put("state", cache.getState());
        response.put("postalCode", cache.getPostalCode());
        response.put("country", cache.getCountry());
        response.put("lat", cache.getLatitude());
        response.put("lng", cache.getLongitude());
        return response;
    }

    /**
     * Round coordinate to 5 decimal places
     */
    private double roundCoordinate(double coordinate) {
        return BigDecimal.valueOf(coordinate)
                .setScale(5, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Clean up expired cache entries
     */
    @Transactional
    public int cleanupExpiredCache() {
        int deleted = cacheRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        logger.info("Cleaned up {} expired geocoding cache entries", deleted);
        return deleted;
    }
}
