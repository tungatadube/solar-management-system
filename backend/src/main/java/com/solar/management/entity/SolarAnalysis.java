package com.solar.management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "solar_analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolarAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 8)
    private Double latitude;

    @Column(precision = 11, scale = 8)
    private Double longitude;

    // Roof measurements
    @Column(name = "roof_area", precision = 10, scale = 2)
    private Double roofArea; // in square meters

    @Column(name = "usable_area", precision = 10, scale = 2)
    private Double usableArea; // in square meters

    @Column(name = "roof_pitch", precision = 5, scale = 2)
    private Double roofPitch; // in degrees

    @Column(name = "roof_orientation", length = 50)
    private String roofOrientation; // North, South, East, West, etc.

    @Column(name = "shading_factor", precision = 3, scale = 2)
    private Double shadingFactor; // 0.0 to 1.0

    // Optimal configuration
    @Column(name = "optimal_azimuth", precision = 5, scale = 2)
    private Double optimalAzimuth; // 0-360 degrees

    @Column(name = "optimal_tilt", precision = 5, scale = 2)
    private Double optimalTilt; // degrees from horizontal

    @Column(name = "number_of_panels")
    private Integer numberOfPanels;

    @Column(name = "system_capacity", precision = 10, scale = 2)
    private Double systemCapacity; // in kW

    @Column(name = "panel_wattage")
    private Integer panelWattage; // watts per panel

    // Production estimates
    @Column(name = "annual_production", precision = 10, scale = 2)
    private Double annualProduction; // kWh per year

    @Column(name = "daily_average", precision = 10, scale = 2)
    private Double dailyAverage; // kWh per day

    @Column(name = "peak_sun_hours", precision = 5, scale = 2)
    private Double peakSunHours; // hours per day

    // Panel layout
    @Column(name = "layout_rows")
    private Integer layoutRows;

    @Column(name = "layout_columns")
    private Integer layoutColumns;

    @Column(name = "panel_spacing", precision = 5, scale = 2)
    private Double panelSpacing; // meters between panels

    // Materials - stored separately for now
    // TODO: Add materials as separate table or JSONB field
    @Transient
    private MaterialRequirements materials;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (analyzedAt == null) {
            analyzedAt = LocalDateTime.now();
        }
    }
}
