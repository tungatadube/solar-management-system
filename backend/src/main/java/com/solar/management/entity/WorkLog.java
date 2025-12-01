package com.solar.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @Column(nullable = false)
    private LocalDate workDate;
    
    @Column(nullable = false)
    private LocalTime startTime;
    
    @Column(nullable = false)
    private LocalTime endTime;
    
    @Column(nullable = false)
    private BigDecimal hoursWorked; // Calculated field
    
    @Column(nullable = false)
    private BigDecimal hourlyRate;
    
    @Column(nullable = false)
    private BigDecimal totalAmount; // hoursWorked * hourlyRate
    
    @Column(columnDefinition = "TEXT")
    private String workDescription;
    
    @Column
    private String jobAddress;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkType workType;
    
    @Column(nullable = false)
    private Boolean invoiced = false;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum WorkType {
        BATTERY_INSTALLATION,
        INVERTER_INSTALLATION,
        CONDUIT_INSTALLATION,
        PREWIRE,
        PANEL_INSTALLATION,
        ELECTRICAL_WORK,
        WAREHOUSE_WORK,
        INSPECTION,
        MAINTENANCE,
        OTHER
    }
    
    @PrePersist
    @PreUpdate
    public void calculateTotals() {
        if (startTime != null && endTime != null) {
            long minutes = java.time.Duration.between(startTime, endTime).toMinutes();
            this.hoursWorked = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        if (hoursWorked != null && hourlyRate != null) {
            this.totalAmount = hoursWorked.multiply(hourlyRate);
        }
    }
}
