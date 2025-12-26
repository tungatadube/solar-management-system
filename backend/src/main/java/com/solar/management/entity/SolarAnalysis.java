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

    private Double latitude;

    private Double longitude;

    // Roof measurements
    @Column(name = "roof_area")
    private Double roofArea; // in square meters

    @Column(name = "usable_area")
    private Double usableArea; // in square meters

    @Column(name = "roof_pitch")
    private Double roofPitch; // in degrees

    @Column(name = "roof_orientation", length = 50)
    private String roofOrientation; // North, South, East, West, etc.

    @Column(name = "shading_factor")
    private Double shadingFactor; // 0.0 to 1.0

    // Optimal configuration
    @Column(name = "optimal_azimuth")
    private Double optimalAzimuth; // 0-360 degrees

    @Column(name = "optimal_tilt")
    private Double optimalTilt; // degrees from horizontal

    @Column(name = "number_of_panels")
    private Integer numberOfPanels;

    @Column(name = "system_capacity")
    private Double systemCapacity; // in kW

    @Column(name = "panel_wattage")
    private Integer panelWattage; // watts per panel

    // Production estimates
    @Column(name = "annual_production")
    private Double annualProduction; // kWh per year

    @Column(name = "daily_average")
    private Double dailyAverage; // kWh per day

    @Column(name = "peak_sun_hours")
    private Double peakSunHours; // hours per day

    // Panel layout
    @Column(name = "layout_rows")
    private Integer layoutRows;

    @Column(name = "layout_columns")
    private Integer layoutColumns;

    @Column(name = "panel_spacing")
    private Double panelSpacing; // meters between panels

    // Roof polygon coordinates (stored as JSON)
    @Column(columnDefinition = "TEXT")
    private String roofPolygonCoordinates; // JSON: [{lat, lng}, ...]

    // Rail cut optimization details (stored as JSON)
    @Column(columnDefinition = "TEXT")
    private String railCutDetails; // JSON: rail cut optimization results

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
