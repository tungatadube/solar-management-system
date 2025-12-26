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
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final WorkLogRepository workLogRepository;
    private final ParameterService parameterService;
    private final FileStorageService fileStorageService;
    
    public Job createJob(Job job) {
        // Generate unique job number
        String jobNumber = generateJobNumber();
        job.setJobNumber(jobNumber);
        job.setStatus(Job.JobStatus.SCHEDULED);

        log.info("Creating new job with number: {}", jobNumber);
        Job savedJob = jobRepository.save(job);

        // Reload the job to ensure location is fully loaded (since it's LAZY)
        savedJob = jobRepository.findById(savedJob.getId()).orElseThrow();

        // Ensure location is loaded by accessing it
        if (savedJob.getLocation() != null) {
            savedJob.getLocation().getAddress(); // Force load
        }

        // Automatically create work logs for all assigned technicians
        if (savedJob.getStartTime() != null && savedJob.getEndTime() != null &&
            savedJob.getAssignedTechnicians() != null && !savedJob.getAssignedTechnicians().isEmpty()) {
            createWorkLogsForJob(savedJob);
            log.info("Created work logs for new job {}", jobNumber);
        } else {
            log.warn("Job {} created without work logs (missing times or technicians)", jobNumber);
        }

        return savedJob;
    }
    
    public Job updateJob(Long id, Job jobDetails) {
        Job job = getJobById(id);

        // Track what changed for cascade updates
        boolean timesChanged = false;
        boolean locationChanged = false;
        boolean descriptionChanged = false;
        boolean typeChanged = false;

        if (jobDetails.getStartTime() != null && !jobDetails.getStartTime().equals(job.getStartTime())) {
            timesChanged = true;
        }
        if (jobDetails.getEndTime() != null && !jobDetails.getEndTime().equals(job.getEndTime())) {
            timesChanged = true;
        }
        if (jobDetails.getLocation() != null &&
            (job.getLocation() == null || !job.getLocation().getId().equals(jobDetails.getLocation().getId()))) {
            locationChanged = true;
        }
        if (jobDetails.getDescription() != null && !jobDetails.getDescription().equals(job.getDescription())) {
            descriptionChanged = true;
        }
        if (jobDetails.getType() != null && !jobDetails.getType().equals(job.getType())) {
            typeChanged = true;
        }

        // Update job fields
        job.setClientName(jobDetails.getClientName());
        job.setClientPhone(jobDetails.getClientPhone());
        job.setClientEmail(jobDetails.getClientEmail());
        job.setType(jobDetails.getType());
        job.setDescription(jobDetails.getDescription());
        job.setStartTime(jobDetails.getStartTime());
        job.setEndTime(jobDetails.getEndTime());
        job.setEstimatedCost(jobDetails.getEstimatedCost());
        job.setSystemSize(jobDetails.getSystemSize());
        job.setNotes(jobDetails.getNotes());

        // Update location if provided
        if (jobDetails.getLocation() != null) {
            job.setLocation(jobDetails.getLocation());
        }

        // Update assigned technicians if provided
        if (jobDetails.getAssignedTechnicians() != null) {
            job.setAssignedTechnicians(jobDetails.getAssignedTechnicians());
        }

        // Save job first
        Job savedJob = jobRepository.save(job);

        // Cascade changes to work logs (only for uninvoiced work logs)
        List<WorkLog> relatedWorkLogs = workLogRepository.findByJob(job);
        if (!relatedWorkLogs.isEmpty()) {
            int updatedCount = 0;
            for (WorkLog workLog : relatedWorkLogs) {
                // Skip invoiced work logs - they should not be modified
                if (workLog.getInvoiced()) {
                    continue;
                }

                boolean workLogUpdated = false;

                if (timesChanged && savedJob.getStartTime() != null && savedJob.getEndTime() != null) {
                    workLog.setStartTime(savedJob.getStartTime().toLocalTime());
                    workLog.setEndTime(savedJob.getEndTime().toLocalTime());
                    workLog.setWorkDate(savedJob.getStartTime().toLocalDate());
                    workLogUpdated = true;
                }

                if (locationChanged && savedJob.getLocation() != null) {
                    workLog.setJobAddress(savedJob.getLocation().getAddress());
                    workLogUpdated = true;
                }

                if (descriptionChanged && savedJob.getDescription() != null) {
                    workLog.setWorkDescription(savedJob.getDescription());
                    workLogUpdated = true;
                }

                if (typeChanged && savedJob.getType() != null) {
                    workLog.setWorkType(determineWorkType(savedJob.getType()));
                    workLogUpdated = true;
                }

                if (workLogUpdated) {
                    workLog.calculateTotals(); // Recalculate hours and amounts
                    workLogRepository.save(workLog);
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                log.info("Cascaded job updates to {} uninvoiced work log(s) for job {}",
                         updatedCount, job.getJobNumber());
            }
        }

        log.info("Updated job {}: location_id={}, technicians={}",
                 savedJob.getJobNumber(),
                 savedJob.getLocation() != null ? savedJob.getLocation().getId() : "null",
                 savedJob.getAssignedTechnicians().size());

        return savedJob;
    }
    
    public Job updateJobStatus(Long id, Job.JobStatus newStatus) {
        Job job = getJobById(id);
        Job.JobStatus oldStatus = job.getStatus();

        job.setStatus(newStatus);

        log.info("Job {} status changed from {} to {}", job.getJobNumber(), oldStatus, newStatus);
        return jobRepository.save(job);
    }

    private void createWorkLogsForJob(Job job) {
        if (job.getAssignedTechnicians() == null || job.getAssignedTechnicians().isEmpty()) {
            log.warn("Job {} has no assigned technicians. No work logs created.", job.getJobNumber());
            return;
        }

        // Get hourly rate from parameters
        BigDecimal hourlyRate = parameterService.getHourlyRate();

        // Calculate hours worked
        Duration duration = Duration.between(job.getStartTime(), job.getEndTime());
        BigDecimal hoursWorked = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        // Calculate total amount
        BigDecimal totalAmount = hoursWorked.multiply(hourlyRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Create work log for each assigned technician
        for (User technician : job.getAssignedTechnicians()) {
            // Determine work type based on job type
            WorkLog.WorkType workType = determineWorkType(job.getType());

            WorkLog workLog = WorkLog.builder()
                    .user(technician)
                    .job(job)
                    .workDate(job.getStartTime().toLocalDate())
                    .startTime(job.getStartTime().toLocalTime())
                    .endTime(job.getEndTime().toLocalTime())
                    .hoursWorked(hoursWorked)
                    .hourlyRate(hourlyRate)
                    .totalAmount(totalAmount)
                    .workType(workType)
                    .workDescription(job.getDescription() != null ? job.getDescription() : job.getType().toString())
                    .jobAddress(job.getLocation() != null ? job.getLocation().getAddress() : "N/A")
                    .invoiced(false)
                    .build();

            workLogRepository.save(workLog);
            log.info("Created work log for technician {} on job {} - {} hours @ ${}/hr = ${}",
                    technician.getUsername(),
                    job.getJobNumber(),
                    hoursWorked,
                    hourlyRate,
                    totalAmount);
        }
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

    private WorkLog.WorkType determineWorkType(Job.JobType jobType) {
        if (jobType == null) {
            return WorkLog.WorkType.OTHER;
        }

        return switch (jobType) {
            case NEW_INSTALLATION -> WorkLog.WorkType.PANEL_INSTALLATION;
            case MAINTENANCE -> WorkLog.WorkType.MAINTENANCE;
            case REPAIR -> WorkLog.WorkType.ELECTRICAL_WORK;
            case INSPECTION -> WorkLog.WorkType.INSPECTION;
            case UPGRADE -> WorkLog.WorkType.ELECTRICAL_WORK;
            default -> WorkLog.WorkType.OTHER;
        };
    }
}
