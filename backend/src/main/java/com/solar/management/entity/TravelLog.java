package com.solar.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Double startLatitude;
    
    @Column(nullable = false)
    private Double startLongitude;
    
    @Column
    private String startAddress;
    
    @Column(nullable = false)
    private Double endLatitude;
    
    @Column(nullable = false)
    private Double endLongitude;
    
    @Column
    private String endAddress;
    
    @Column(nullable = false)
    private LocalDateTime departureTime;
    
    @Column
    private LocalDateTime arrivalTime;
    
    @Column
    private BigDecimal distance; // in kilometers
    
    @Column
    private Integer duration; // in minutes
    
    @Column
    private BigDecimal fuelCost;
    
    @Column
    private String vehicleRegistration;
    
    @Column
    private String notes;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
