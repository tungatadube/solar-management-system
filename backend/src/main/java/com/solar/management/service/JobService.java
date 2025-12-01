package com.solar.management.service;

import com.solar.management.entity.*;
import com.solar.management.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JobService {
    
    private final JobRepository jobRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final JobImageRepository jobImageRepository;
    private final JobStockRepository jobStockRepository;
    private final TravelLogRepository travelLogRepository;
    private final FileStorageService fileStorageService;
    
    public Job createJob(Job job) {
        // Generate unique job number
        String jobNumber = generateJobNumber();
        job.setJobNumber(jobNumber);
        job.setStatus(Job.JobStatus.SCHEDULED);
        
        log.info("Creating new job with number: {}", jobNumber);
        return jobRepository.save(job);
    }
    
    public Job updateJob(Long id, Job jobDetails) {
        Job job = getJobById(id);
        
        job.setClientName(jobDetails.getClientName());
        job.setClientPhone(jobDetails.getClientPhone());
        job.setClientEmail(jobDetails.getClientEmail());
        job.setDescription(jobDetails.getDescription());
        job.setScheduledStartTime(jobDetails.getScheduledStartTime());
        job.setScheduledEndTime(jobDetails.getScheduledEndTime());
        job.setEstimatedCost(jobDetails.getEstimatedCost());
        job.setSystemSize(jobDetails.getSystemSize());
        job.setNotes(jobDetails.getNotes());
        
        return jobRepository.save(job);
    }
    
    public Job updateJobStatus(Long id, Job.JobStatus newStatus) {
        Job job = getJobById(id);
        Job.JobStatus oldStatus = job.getStatus();
        
        job.setStatus(newStatus);
        
        // Automatically set start/end times based on status changes
        if (newStatus == Job.JobStatus.IN_PROGRESS && oldStatus == Job.JobStatus.SCHEDULED) {
            job.setActualStartTime(LocalDateTime.now());
        } else if (newStatus == Job.JobStatus.COMPLETED && oldStatus == Job.JobStatus.IN_PROGRESS) {
            job.setActualEndTime(LocalDateTime.now());
        }
        
        log.info("Job {} status changed from {} to {}", job.getJobNumber(), oldStatus, newStatus);
        return jobRepository.save(job);
    }
    
    public JobImage uploadJobImage(Long jobId, MultipartFile file, JobImage.ImageType imageType, 
                                   String caption, Double latitude, Double longitude) {
        Job job = getJobById(jobId);
        
        // Upload file to storage
        String imageUrl = fileStorageService.storeFile(file, "jobs/" + jobId);
        
        JobImage jobImage = JobImage.builder()
                .job(job)
                .imageUrl(imageUrl)
                .fileName(file.getOriginalFilename())
                .imageType(imageType)
                .caption(caption)
                .latitude(latitude)
                .longitude(longitude)
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();
        
        log.info("Uploaded image for job {}: {}", job.getJobNumber(), file.getOriginalFilename());
        return jobImageRepository.save(jobImage);
    }
    
    public void assignStockToJob(Long jobId, Long stockItemId, Integer quantity) {
        Job job = getJobById(jobId);
        // This would interact with StockItem and create JobStock entry
        // Implementation would include stock validation and deduction
        log.info("Assigned {} units of stock {} to job {}", quantity, stockItemId, job.getJobNumber());
    }
    
    public TravelLog logTravel(TravelLog travelLog) {
        // Calculate duration if arrival time is set
        if (travelLog.getArrivalTime() != null) {
            long minutes = java.time.Duration.between(
                travelLog.getDepartureTime(), 
                travelLog.getArrivalTime()
            ).toMinutes();
            travelLog.setDuration((int) minutes);
        }
        
        log.info("Travel log created for job {} by user {}", 
                 travelLog.getJob().getJobNumber(), 
                 travelLog.getUser().getUsername());
        return travelLogRepository.save(travelLog);
    }
    
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
    }
    
    public Job getJobByJobNumber(String jobNumber) {
        return jobRepository.findByJobNumber(jobNumber)
                .orElseThrow(() -> new RuntimeException("Job not found with number: " + jobNumber));
    }
    
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }
    
    public List<Job> getJobsByStatus(Job.JobStatus status) {
        return jobRepository.findByStatus(status);
    }
    
    public List<Job> getJobsByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return jobRepository.findByAssignedTo(user);
    }
    
    public Page<Job> searchJobsByClientName(String clientName, Pageable pageable) {
        return jobRepository.findByClientNameContainingIgnoreCase(clientName, pageable);
    }
    
    public List<JobImage> getJobImages(Long jobId) {
        Job job = getJobById(jobId);
        return jobImageRepository.findByJob(job);
    }
    
    public List<TravelLog> getJobTravelLogs(Long jobId) {
        Job job = getJobById(jobId);
        return travelLogRepository.findByJob(job);
    }
    
    public BigDecimal calculateJobStockCost(Long jobId) {
        Job job = getJobById(jobId);
        Double totalCost = jobStockRepository.calculateTotalCostForJob(job);
        return totalCost != null ? BigDecimal.valueOf(totalCost) : BigDecimal.ZERO;
    }
    
    public void deleteJob(Long id) {
        Job job = getJobById(id);
        log.info("Deleting job: {}", job.getJobNumber());
        jobRepository.delete(job);
    }
    
    private String generateJobNumber() {
        String prefix = "JOB";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + timestamp.substring(timestamp.length() - 8) + "-" + random;
    }
}
