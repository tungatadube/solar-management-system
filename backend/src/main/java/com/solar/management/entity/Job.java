package com.solar.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"assignedTechnicians", "jobStocks", "jobImages", "travelLogs"})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Job {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String jobNumber;
    
    @Column(nullable = false)
    private String clientName;
    
    @Column
    private String clientPhone;
    
    @Column
    private String clientEmail;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "job_technicians",
        joinColumns = @JoinColumn(name = "job_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedTechnicians = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType type;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column
    private LocalDateTime scheduledStartTime;
    
    @Column
    private LocalDateTime scheduledEndTime;
    
    @Column
    private LocalDateTime actualStartTime;
    
    @Column
    private LocalDateTime actualEndTime;
    
    @Column
    private BigDecimal estimatedCost;
    
    @Column
    private BigDecimal actualCost;
    
    @Column
    private Integer systemSize; // in kW
    
    @Column
    private String notes;
    
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<JobStock> jobStocks = new HashSet<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<JobImage> jobImages = new HashSet<>();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<TravelLog> travelLogs = new HashSet<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum JobStatus {
        SCHEDULED,
        IN_PROGRESS,
        ON_HOLD,
        COMPLETED,
        CANCELLED
    }
    
    public enum JobType {
        NEW_INSTALLATION,
        MAINTENANCE,
        REPAIR,
        INSPECTION,
        UPGRADE
    }
}
