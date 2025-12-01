package com.solar.management.repository;

import com.solar.management.entity.User;
import com.solar.management.entity.WorkLog;
import com.solar.management.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkLogRepository extends JpaRepository<WorkLog, Long> {
    
    List<WorkLog> findByUser(User user);
    
    List<WorkLog> findByJob(Job job);
    
    List<WorkLog> findByInvoiced(Boolean invoiced);
    
    @Query("SELECT w FROM WorkLog w WHERE w.user = :user AND w.workDate BETWEEN :startDate AND :endDate")
    List<WorkLog> findByUserAndDateRange(@Param("user") User user, 
                                         @Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);
    
    @Query("SELECT w FROM WorkLog w WHERE w.user = :user AND w.invoiced = false ORDER BY w.workDate ASC")
    List<WorkLog> findUninvoicedWorkByUser(@Param("user") User user);
    
    @Query("SELECT w FROM WorkLog w WHERE w.user = :user AND w.invoiced = false AND w.workDate BETWEEN :startDate AND :endDate ORDER BY w.workDate ASC")
    List<WorkLog> findUninvoicedWorkByUserAndDateRange(@Param("user") User user,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);
}
