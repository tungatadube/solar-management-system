package com.solar.management.service;

import com.solar.management.entity.Job;
import com.solar.management.entity.User;
import com.solar.management.entity.WorkLog;
import com.solar.management.repository.JobRepository;
import com.solar.management.repository.UserRepository;
import com.solar.management.repository.WorkLogRepository;
import com.solar.management.security.AuthenticationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service layer for WorkLog operations with role-based access control
 * Ensures technicians can only access their own work logs
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkLogService {

    private final WorkLogRepository workLogRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final AuthenticationHelper authHelper;

    /**
     * Get all work logs with role-based filtering
     * ADMIN/MANAGER: All work logs
     * TECHNICIAN: Only own work logs
     */
    public List<WorkLog> getAllWorkLogsForCurrentUser() {
        if (authHelper.isAdminOrManager()) {
            return workLogRepository.findAll();
        } else {
            User currentUser = authHelper.getCurrentUser();
            return workLogRepository.findByUser(currentUser);
        }
    }

    /**
     * Get work log by ID with access validation
     * @param id Work log ID
     * @return Work log if user has access
     * @throws org.springframework.security.access.AccessDeniedException if technician doesn't own the work log
     */
    public WorkLog getWorkLogByIdWithAuth(Long id) {
        WorkLog workLog = workLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work log not found with id: " + id));
        authHelper.validateWorkLogAccess(workLog);
        return workLog;
    }

    /**
     * Get work logs by user with access validation
     * Technicians can only access their own userId
     * @param userId User ID to query
     * @return List of work logs for the user
     * @throws org.springframework.security.access.AccessDeniedException if technician tries to access another user's data
     */
    public List<WorkLog> getWorkLogsByUserWithAuth(Long userId) {
        authHelper.validateUserAccess(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return workLogRepository.findByUser(user);
    }

    /**
     * Get work logs by job with access validation
     * Technicians can only access jobs they're assigned to
     * @param jobId Job ID to query
     * @return List of work logs for the job
     * @throws org.springframework.security.access.AccessDeniedException if technician not assigned to job
     */
    public List<WorkLog> getWorkLogsByJobWithAuth(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + jobId));
        authHelper.validateJobAccess(job);
        return workLogRepository.findByJob(job);
    }

    /**
     * Get uninvoiced work logs with access validation
     * @param userId User ID to query
     * @return List of uninvoiced work logs
     * @throws org.springframework.security.access.AccessDeniedException if technician tries to access another user's data
     */
    public List<WorkLog> getUninvoicedWorkLogsWithAuth(Long userId) {
        authHelper.validateUserAccess(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return workLogRepository.findUninvoicedWorkByUser(user);
    }

    /**
     * Get work logs by user and date range with access validation
     * @param userId User ID to query
     * @param startDate Start date
     * @param endDate End date
     * @return List of work logs in the date range
     * @throws org.springframework.security.access.AccessDeniedException if technician tries to access another user's data
     */
    public List<WorkLog> getWorkLogsByUserAndDateRangeWithAuth(
            Long userId, LocalDate startDate, LocalDate endDate) {
        authHelper.validateUserAccess(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return workLogRepository.findByUserAndDateRange(user, startDate, endDate);
    }

    /**
     * Update work log with access validation
     * Technicians can only update their own work logs
     * Cannot update invoiced work logs
     * @param id Work log ID
     * @param workLogDetails Updated work log data
     * @return Updated work log
     * @throws RuntimeException if work log is already invoiced
     * @throws org.springframework.security.access.AccessDeniedException if technician doesn't own the work log
     */
    public WorkLog updateWorkLogWithAuth(Long id, WorkLog workLogDetails) {
        WorkLog workLog = getWorkLogByIdWithAuth(id); // validates access

        // Prevent updating if already invoiced
        if (workLog.getInvoiced()) {
            throw new RuntimeException("Cannot update work log that has already been invoiced");
        }

        // Update only allowed fields
        if (workLogDetails.getWorkDescription() != null) {
            workLog.setWorkDescription(workLogDetails.getWorkDescription());
        }
        if (workLogDetails.getWorkType() != null) {
            workLog.setWorkType(workLogDetails.getWorkType());
        }

        log.info("Updated work log {} by user {}", id, authHelper.getCurrentUser().getUsername());
        return workLogRepository.save(workLog);
    }
}
