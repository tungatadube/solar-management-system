package com.solar.management.controller;

import com.solar.management.entity.Job;
import com.solar.management.entity.JobImage;
import com.solar.management.entity.TravelLog;
import com.solar.management.security.AuthenticationHelper;
import com.solar.management.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobController {

    private final JobService jobService;
    private final AuthenticationHelper authHelper;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        Job createdJob = jobService.createJob(job);
        return new ResponseEntity<>(createdJob, HttpStatus.CREATED);
    }
    
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Job>> getAllJobs() {
        List<Job> jobs = jobService.getAllJobsForCurrentUser();
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        Job job = jobService.getJobByIdWithAuth(id);
        return ResponseEntity.ok(job);
    }
    
    @GetMapping("/number/{jobNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Job> getJobByJobNumber(@PathVariable String jobNumber) {
        Job job = jobService.getJobByJobNumber(jobNumber);
        authHelper.validateJobAccess(job);
        return ResponseEntity.ok(job);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Job>> getJobsByStatus(@PathVariable Job.JobStatus status) {
        List<Job> jobs = jobService.getJobsByStatusForCurrentUser(status);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Job>> getJobsByUser(@PathVariable Long userId) {
        authHelper.validateUserAccess(userId);
        List<Job> jobs = jobService.getJobsByUser(userId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Job>> searchJobsByClient(
            @RequestParam String clientName,
            Pageable pageable) {
        Page<Job> jobs = jobService.searchJobsByClientNameForCurrentUser(clientName, pageable);
        return ResponseEntity.ok(jobs);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        Job updatedJob = jobService.updateJob(id, job);
        return ResponseEntity.ok(updatedJob);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Job> updateJobStatus(
            @PathVariable Long id,
            @RequestParam Job.JobStatus status) {
        Job job = jobService.getJobByIdWithAuth(id);
        Job updatedJob = jobService.updateJobStatus(id, status);
        return ResponseEntity.ok(updatedJob);
    }
    
    @PostMapping("/{id}/images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<JobImage> uploadJobImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageType") JobImage.ImageType imageType,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "latitude", required = false) Double longitude,
            @RequestParam(value = "longitude", required = false) Double latitude) {

        Job job = jobService.getJobByIdWithAuth(id);
        JobImage jobImage = jobService.uploadJobImage(id, file, imageType, caption, latitude, longitude);
        return new ResponseEntity<>(jobImage, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/images")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<JobImage>> getJobImages(@PathVariable Long id) {
        Job job = jobService.getJobByIdWithAuth(id);
        List<JobImage> images = jobService.getJobImages(id);
        return ResponseEntity.ok(images);
    }

    @PostMapping("/{id}/travel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TravelLog> logTravel(@PathVariable Long id, @RequestBody TravelLog travelLog) {
        Job job = jobService.getJobByIdWithAuth(id);
        TravelLog savedLog = jobService.logTravel(travelLog);
        return new ResponseEntity<>(savedLog, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/travel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TravelLog>> getJobTravelLogs(@PathVariable Long id) {
        Job job = jobService.getJobByIdWithAuth(id);
        List<TravelLog> travelLogs = jobService.getJobTravelLogs(id);
        return ResponseEntity.ok(travelLogs);
    }

    @GetMapping("/{id}/stock-cost")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BigDecimal> calculateJobStockCost(@PathVariable Long id) {
        Job job = jobService.getJobByIdWithAuth(id);
        BigDecimal totalCost = jobService.calculateJobStockCost(id);
        return ResponseEntity.ok(totalCost);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
