package com.solar.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @Column(nullable = false)
    private String imageUrl;
    
    @Column(nullable = false)
    private String fileName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImageType imageType;
    
    @Column(columnDefinition = "TEXT")
    private String caption;
    
    @Column
    private Double latitude;
    
    @Column
    private Double longitude;
    
    @Column
    private Long fileSize;
    
    @Column
    private String mimeType;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    
    public enum ImageType {
        BEFORE_INSTALLATION,
        DURING_INSTALLATION,
        AFTER_INSTALLATION,
        ELECTRICAL_WORK,
        MOUNTING,
        INVERTER,
        BATTERY,
        METER,
        DOCUMENTATION,
        OTHER
    }
}
