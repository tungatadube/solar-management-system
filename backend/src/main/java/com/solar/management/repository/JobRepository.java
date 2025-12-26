package com.solar.management.repository;

import com.solar.management.entity.Job;
import com.solar.management.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByJobNumber(String jobNumber);
    List<Job> findByStatus(Job.JobStatus status);

    @Query("SELECT j FROM Job j JOIN j.assignedTechnicians t WHERE t = :user")
    List<Job> findByAssignedTo(@Param("user") User user);

    @Query("SELECT j FROM Job j JOIN j.assignedTechnicians t WHERE t = :user AND j.status IN :statuses")
    List<Job> findByAssignedToAndStatusIn(@Param("user") User user, @Param("statuses") List<Job.JobStatus> statuses);

    @Query("SELECT j FROM Job j WHERE j.startTime BETWEEN :start AND :end")
    List<Job> findJobsBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Page<Job> findByClientNameContainingIgnoreCase(String clientName, Pageable pageable);

    @Query("SELECT j FROM Job j JOIN j.assignedTechnicians t WHERE t = :user AND LOWER(j.clientName) LIKE LOWER(CONCAT('%', :clientName, '%'))")
    Page<Job> findByClientNameContainingIgnoreCaseAndAssignedTo(
            @Param("clientName") String clientName,
            @Param("user") User user,
            Pageable pageable);
}
