package com.solar.management.controller;

import com.solar.management.entity.WorkLog;
import com.solar.management.repository.WorkLogRepository;
import com.solar.management.repository.UserRepository;
import com.solar.management.repository.JobRepository;
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
    
    /**
     * Create a new work log
     */
    @PostMapping
    public ResponseEntity<WorkLog> createWorkLog(@RequestBody WorkLog workLog) {
        workLog.calculateTotals(); // Ensure calculations are done
        WorkLog saved = workLogRepository.save(workLog);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
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
     */
    @PutMapping("/{id}")
    public ResponseEntity<WorkLog> updateWorkLog(@PathVariable Long id, @RequestBody WorkLog workLogDetails) {
        WorkLog workLog = workLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work log not found"));
        
        if (workLogDetails.getWorkDate() != null) workLog.setWorkDate(workLogDetails.getWorkDate());
        if (workLogDetails.getStartTime() != null) workLog.setStartTime(workLogDetails.getStartTime());
        if (workLogDetails.getEndTime() != null) workLog.setEndTime(workLogDetails.getEndTime());
        if (workLogDetails.getHourlyRate() != null) workLog.setHourlyRate(workLogDetails.getHourlyRate());
        if (workLogDetails.getWorkDescription() != null) workLog.setWorkDescription(workLogDetails.getWorkDescription());
        if (workLogDetails.getJobAddress() != null) workLog.setJobAddress(workLogDetails.getJobAddress());
        if (workLogDetails.getWorkType() != null) workLog.setWorkType(workLogDetails.getWorkType());
        
        workLog.calculateTotals();
        
        WorkLog updated = workLogRepository.save(workLog);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * Delete work log
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkLog(@PathVariable Long id) {
        WorkLog workLog = workLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work log not found"));
        
        if (workLog.getInvoiced()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        workLogRepository.delete(workLog);
        return ResponseEntity.noContent().build();
    }
}
