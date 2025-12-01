package com.solar.management.repository;

import com.solar.management.entity.Job;
import com.solar.management.entity.TravelLog;
import com.solar.management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TravelLogRepository extends JpaRepository<TravelLog, Long> {
    List<TravelLog> findByJob(Job job);
    List<TravelLog> findByUser(User user);

    @Query("SELECT t FROM TravelLog t WHERE t.user = :user AND t.departureTime BETWEEN :start AND :end")
    List<TravelLog> findByUserAndDateRange(@Param("user") User user,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    @Query("SELECT SUM(t.distance) FROM TravelLog t WHERE t.user = :user AND t.departureTime BETWEEN :start AND :end")
    Double calculateTotalDistanceForUser(@Param("user") User user,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}
