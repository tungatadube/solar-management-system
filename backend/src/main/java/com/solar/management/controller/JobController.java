package com.solar.management.controller;

import com.solar.management.entity.Job;
import com.solar.management.entity.JobImage;
import com.solar.management.entity.TravelLog;
import com.solar.management.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    
    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        Job createdJob = jobService.createJob(job);
        return new ResponseEntity<>(createdJob, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        List<Job> jobs = jobService.getAllJobs();
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        Job job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }
    
    @GetMapping("/number/{jobNumber}")
    public ResponseEntity<Job> getJobByJobNumber(@PathVariable String jobNumber) {
        Job job = jobService.getJobByJobNumber(jobNumber);
        return ResponseEntity.ok(job);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Job>> getJobsByStatus(@PathVariable Job.JobStatus status) {
        List<Job> jobs = jobService.getJobsByStatus(status);
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Job>> getJobsByUser(@PathVariable Long userId) {
        List<Job> jobs = jobService.getJobsByUser(userId);
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<Job>> searchJobsByClient(
            @RequestParam String clientName,
            Pageable pageable) {
        Page<Job> jobs = jobService.searchJobsByClientName(clientName, pageable);
        return ResponseEntity.ok(jobs);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable Long id, @RequestBody Job job) {
        Job updatedJob = jobService.updateJob(id, job);
        return ResponseEntity.ok(updatedJob);
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Job> updateJobStatus(
            @PathVariable Long id,
            @RequestParam Job.JobStatus status) {
        Job updatedJob = jobService.updateJobStatus(id, status);
        return ResponseEntity.ok(updatedJob);
    }
    
    @PostMapping("/{id}/images")
    public ResponseEntity<JobImage> uploadJobImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageType") JobImage.ImageType imageType,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) {
        
        JobImage jobImage = jobService.uploadJobImage(id, file, imageType, caption, latitude, longitude);
        return new ResponseEntity<>(jobImage, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}/images")
    public ResponseEntity<List<JobImage>> getJobImages(@PathVariable Long id) {
        List<JobImage> images = jobService.getJobImages(id);
        return ResponseEntity.ok(images);
    }
    
    @PostMapping("/{id}/travel")
    public ResponseEntity<TravelLog> logTravel(@PathVariable Long id, @RequestBody TravelLog travelLog) {
        TravelLog savedLog = jobService.logTravel(travelLog);
        return new ResponseEntity<>(savedLog, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}/travel")
    public ResponseEntity<List<TravelLog>> getJobTravelLogs(@PathVariable Long id) {
        List<TravelLog> travelLogs = jobService.getJobTravelLogs(id);
        return ResponseEntity.ok(travelLogs);
    }
    
    @GetMapping("/{id}/stock-cost")
    public ResponseEntity<BigDecimal> calculateJobStockCost(@PathVariable Long id) {
        BigDecimal totalCost = jobService.calculateJobStockCost(id);
        return ResponseEntity.ok(totalCost);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
