package com.solar.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "location_tracking", indexes = {
    @Index(name = "idx_user_timestamp", columnList = "user_id,timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Double latitude;
    
    @Column(nullable = false)
    private Double longitude;
    
    @Column
    private Double accuracy; // in meters
    
    @Column
    private Double altitude;
    
    @Column
    private Double speed; // in m/s
    
    @Column
    private Double heading; // bearing in degrees
    
    @Column
    private String address;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column
    private String deviceId;
    
    @Column
    private String notes;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
