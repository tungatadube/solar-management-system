package com.solar.management.controller;

import com.solar.management.entity.WorkLog;
import com.solar.management.service.WorkLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/worklogs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkLogController {

    private final WorkLogService workLogService;
    
    /**
     * Create a new work log
     * DISABLED: Work logs are automatically created when a Job is marked as COMPLETED.
     * To create work logs, update the job status through the Job API.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createWorkLog(@RequestBody WorkLog workLog) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Work logs cannot be created directly. They are automatically generated when a Job is marked as COMPLETED.");
    }

    /**
     * Get all work logs with role-based filtering
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkLog>> getAllWorkLogs() {
        List<WorkLog> workLogs = workLogService.getAllWorkLogsForCurrentUser();
        return ResponseEntity.ok(workLogs);
    }

    /**
     * Get work log by ID with access validation
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkLog> getWorkLogById(@PathVariable Long id) {
        WorkLog workLog = workLogService.getWorkLogByIdWithAuth(id);
        return ResponseEntity.ok(workLog);
    }

    /**
     * Get work logs for a specific user with access validation
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkLog>> getWorkLogsByUser(@PathVariable Long userId) {
        List<WorkLog> workLogs = workLogService.getWorkLogsByUserWithAuth(userId);
        return ResponseEntity.ok(workLogs);
    }

    /**
     * Get work logs for a specific job with access validation
     */
    @GetMapping("/job/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkLog>> getWorkLogsByJob(@PathVariable Long jobId) {
        List<WorkLog> workLogs = workLogService.getWorkLogsByJobWithAuth(jobId);
        return ResponseEntity.ok(workLogs);
    }

    /**
     * Get uninvoiced work logs for a user with access validation
     */
    @GetMapping("/user/{userId}/uninvoiced")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkLog>> getUninvoicedWorkLogs(@PathVariable Long userId) {
        List<WorkLog> workLogs = workLogService.getUninvoicedWorkLogsWithAuth(userId);
        return ResponseEntity.ok(workLogs);
    }

    /**
     * Get work logs for a user within a date range with access validation
     */
    @GetMapping("/user/{userId}/date-range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkLog>> getWorkLogsByUserAndDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<WorkLog> workLogs = workLogService.getWorkLogsByUserAndDateRangeWithAuth(
                userId, startDate, endDate);
        return ResponseEntity.ok(workLogs);
    }

    /**
     * Update work log with access validation
     * NOTE: Can only update work details (description, type).
     * Job relationship, dates, and times are managed through the Job entity.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateWorkLog(@PathVariable Long id, @RequestBody WorkLog workLogDetails) {
        try {
            WorkLog updated = workLogService.updateWorkLogWithAuth(id, workLogDetails);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    /**
     * Delete work log
     * DISABLED: Work logs can only be deleted through the parent Job.
     * Deleting a Job will cascade delete all related work logs.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteWorkLog(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Work logs cannot be deleted directly. Delete the parent Job to remove all associated work logs.");
    }
}
