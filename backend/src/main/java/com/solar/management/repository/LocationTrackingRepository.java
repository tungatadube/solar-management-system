package com.solar.management.repository;

import com.solar.management.entity.LocationTracking;
import com.solar.management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationTrackingRepository extends JpaRepository<LocationTracking, Long> {
    List<LocationTracking> findByUserOrderByTimestampDesc(User user);

    @Query("SELECT l FROM LocationTracking l WHERE l.user = :user AND l.timestamp BETWEEN :start AND :end ORDER BY l.timestamp DESC")
    List<LocationTracking> findByUserAndTimestampBetween(@Param("user") User user,
                                                          @Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);

    @Query("SELECT l FROM LocationTracking l WHERE l.user = :user ORDER BY l.timestamp DESC LIMIT 1")
    Optional<LocationTracking> findLatestLocationForUser(@Param("user") User user);
}
