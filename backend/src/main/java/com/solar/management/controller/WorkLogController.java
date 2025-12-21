package com.solar.management.controller;

import com.solar.management.entity.WorkLog;
import com.solar.management.repository.WorkLogRepository;
import com.solar.management.repository.UserRepository;
import com.solar.management.repository.JobRepository;
import com.solar.management.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/worklogs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkLogController {

    private final WorkLogRepository workLogRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final InvoiceRepository invoiceRepository;
    
    /**
     * Create a new work log
     * DISABLED: Work logs are automatically created when a Job is marked as COMPLETED.
     * To create work logs, update the job status through the Job API.
     */
    @PostMapping
    public ResponseEntity<?> createWorkLog(@RequestBody WorkLog workLog) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Work logs cannot be created directly. They are automatically generated when a Job is marked as COMPLETED.");
    }
    
    /**
     * Get all work logs
     */
    @GetMapping
    public ResponseEntity<List<WorkLog>> getAllWorkLogs() {
        List<WorkLog> workLogs = workLogRepository.findAll();
        return ResponseEntity.ok(workLogs);
    }
    
    /**
     * Get work log by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkLog> getWorkLogById(@PathVariable Long id) {
        WorkLog workLog = workLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work log not found"));
        return ResponseEntity.ok(workLog);
    }
    
    /**
     * Get work logs for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<WorkLog>> getWorkLogsByUser(@PathVariable Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<WorkLog> workLogs = workLogRepository.findByUser(user);
        return ResponseEntity.ok(workLogs);
    }
    
    /**
     * Get work logs for a specific job
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<WorkLog>> getWorkLogsByJob(@PathVariable Long jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        List<WorkLog> workLogs = workLogRepository.findByJob(job);
        return ResponseEntity.ok(workLogs);
    }
    
    /**
     * Get uninvoiced work logs for a user
     */
    @GetMapping("/user/{userId}/uninvoiced")
    public ResponseEntity<List<WorkLog>> getUninvoicedWorkLogs(@PathVariable Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<WorkLog> workLogs = workLogRepository.findUninvoicedWorkByUser(user);
        return ResponseEntity.ok(workLogs);
    }
    
    /**
     * Get work logs for a user within a date range
     */
    @GetMapping("/user/{userId}/date-range")
    public ResponseEntity<List<WorkLog>> getWorkLogsByUserAndDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<WorkLog> workLogs = workLogRepository.findByUserAndDateRange(user, startDate, endDate);
        return ResponseEntity.ok(workLogs);
    }
    
    /**
     * Update work log
     * NOTE: Can only update work details (description, type).
     * Job relationship, dates, and times are managed through the Job entity.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkLog(@PathVariable Long id, @RequestBody WorkLog workLogDetails) {
        WorkLog workLog = workLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work log not found"));

        // Prevent updating if already invoiced
        if (workLog.getInvoiced()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Cannot update work log that has already been invoiced.");
        }

        // Only allow updating description and work type (not job relationship, dates, or times)
        if (workLogDetails.getWorkDescription() != null) {
            workLog.setWorkDescription(workLogDetails.getWorkDescription());
        }
        if (workLogDetails.getWorkType() != null) {
            workLog.setWorkType(workLogDetails.getWorkType());
        }

        // Job, user, dates, times, hourlyRate, and jobAddress are managed through Job entity
        // and should not be updated directly

        WorkLog updated = workLogRepository.save(workLog);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete work log
     * DISABLED: Work logs can only be deleted through the parent Job.
     * Deleting a Job will cascade delete all related work logs.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkLog(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Work logs cannot be deleted directly. Delete the parent Job to remove all associated work logs.");
    }
}
