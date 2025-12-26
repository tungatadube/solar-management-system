package com.solar.management.repository;

import com.solar.management.entity.GeocodingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GeocodingCacheRepository extends JpaRepository<GeocodingCache, Long> {

    /**
     * Find cached geocoding result by coordinates
     * @param latitude Latitude rounded to 5 decimal places
     * @param longitude Longitude rounded to 5 decimal places
     * @return Optional containing the cached result if found
     */
    Optional<GeocodingCache> findByLatitudeAndLongitude(Double latitude, Double longitude);

    /**
     * Delete all expired cache entries
     * @param now Current timestamp
     * @return Number of deleted entries
     */
    @Modifying
    @Query("DELETE FROM GeocodingCache g WHERE g.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") LocalDateTime now);
}
