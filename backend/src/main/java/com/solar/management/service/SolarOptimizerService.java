package com.solar.management.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.solar.management.entity.Job;
import com.solar.management.entity.MaterialRequirements;
import com.solar.management.entity.SolarAnalysis;
import com.solar.management.repository.JobRepository;
import com.solar.management.repository.SolarAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SolarOptimizerService {

    private final SolarAnalysisRepository solarAnalysisRepository;
    private final JobRepository jobRepository;
    private final SolarCalculationService calculationService;
    private final MaterialCalculationService materialCalculationService;
    private final ObjectMapper objectMapper;

    private static final double STANDARD_PANEL_WATTAGE = 330.0;
    private static final double USABLE_ROOF_PERCENTAGE = 0.80; // 80% of roof is usable

    /**
     * Perform complete solar analysis for a location
     */
    public SolarAnalysis performAnalysis(
        Long jobId,
        String address,
        Double latitude,
        Double longitude,
        Double roofArea,
        Double targetCapacity,
        String roofType,
        java.util.List<com.solar.management.controller.SolarOptimizerController.Coordinate> coordinates
    ) {
        log.info("Performing solar analysis for address: {}, capacity: {}kW", address, targetCapacity);

        // Get job if provided
        Job job = null;
        if (jobId != null) {
            job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        }

        // Calculate actual roof azimuth from polygon coordinates
        double actualRoofAzimuth = calculationService.calculateRoofAzimuth(coordinates);

        // If we couldn't calculate from coordinates, use optimal azimuth as fallback
        double optimalAzimuth = calculationService.calculateOptimalAzimuth(latitude);
        double roofAzimuth = (actualRoofAzimuth >= 0) ? actualRoofAzimuth : optimalAzimuth;

        double optimalTilt = calculationService.calculateOptimalTilt(latitude);

        // Calculate number of panels needed
        int numberOfPanels = calculationService.calculatePanelQuantity(
            targetCapacity,
            STANDARD_PANEL_WATTAGE
        );

        // Check if panels fit on roof
        int maxPanels = calculationService.calculateMaxPanels(roofArea, USABLE_ROOF_PERCENTAGE);
        if (numberOfPanels > maxPanels) {
            log.warn("Requested {} panels but only {} fit on roof", numberOfPanels, maxPanels);
            numberOfPanels = maxPanels;
        }

        // Calculate actual system capacity based on panels that fit
        double actualCapacity = (numberOfPanels * STANDARD_PANEL_WATTAGE) / 1000.0;

        // Calculate production estimates
        double peakSunHours = calculationService.calculatePeakSunHours(latitude);
        double shadingFactor = calculationService.calculateShadingFactor(latitude, longitude);

        double theoreticalProduction = calculationService.estimateAnnualProduction(
            numberOfPanels,
            STANDARD_PANEL_WATTAGE,
            peakSunHours,
            0.85
        );

        double annualProduction = calculationService.calculateActualProduction(
            theoreticalProduction,
            shadingFactor
        );

        double dailyAverage = calculationService.calculateDailyAverage(annualProduction);

        // Calculate optimal panel layout
        double roofWidth = Math.sqrt(roofArea); // Simplified: assume squareish roof
        double roofLength = roofArea / roofWidth;
        int[] layout = calculationService.optimizePanelLayout(roofWidth, roofLength, numberOfPanels);

        // Calculate materials with layout info for rail optimization
        MaterialRequirements materials = materialCalculationService.calculateMaterials(
            numberOfPanels,
            actualCapacity,
            roofType,
            "flush-mount",
            layout[0],  // layoutRows
            layout[1],  // layoutColumns
            0.05        // panelSpacing
        );

        // Calculate roof details
        double roofPitch = calculationService.estimateRoofPitch(roofType);
        String roofOrientation = calculationService.getOrientationFromAzimuth(roofAzimuth);
        double usableArea = roofArea * USABLE_ROOF_PERCENTAGE;

        log.info("Roof orientation: {} ({}°), Optimal for location would be: {} ({}°)",
                roofOrientation, roofAzimuth,
                calculationService.getOrientationFromAzimuth(optimalAzimuth), optimalAzimuth);

        // Serialize polygon coordinates to JSON
        String coordinatesJson = null;
        if (coordinates != null && !coordinates.isEmpty()) {
            try {
                coordinatesJson = objectMapper.writeValueAsString(coordinates);
            } catch (Exception e) {
                log.error("Failed to serialize roof polygon coordinates", e);
            }
        }

        // Create analysis entity
        SolarAnalysis analysis = SolarAnalysis.builder()
            .job(job)
            .address(address)
            .latitude(latitude)
            .longitude(longitude)
            .roofArea(roofArea)
            .usableArea(usableArea)
            .roofPitch(roofPitch)
            .roofOrientation(roofOrientation)
            .shadingFactor(shadingFactor)
            .optimalAzimuth(optimalAzimuth)
            .optimalTilt(optimalTilt)
            .numberOfPanels(numberOfPanels)
            .systemCapacity(actualCapacity)
            .panelWattage((int) STANDARD_PANEL_WATTAGE)
            .annualProduction(annualProduction)
            .dailyAverage(dailyAverage)
            .peakSunHours(peakSunHours)
            .layoutRows(layout[0])
            .layoutColumns(layout[1])
            .panelSpacing(0.05)
            .roofPolygonCoordinates(coordinatesJson)
            .railCutDetails(materials.getRailCutPlan())
            .materials(materials)
            .analyzedAt(LocalDateTime.now())
            .build();

        SolarAnalysis saved = solarAnalysisRepository.save(analysis);

        log.info("Solar analysis complete: {} panels, {}kW system, {}kWh/year",
            numberOfPanels, actualCapacity, annualProduction);

        return saved;
    }

    /**
     * Get solar analysis by ID
     */
    public SolarAnalysis getAnalysisById(Long id) {
        return solarAnalysisRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Solar analysis not found"));
    }

    /**
     * Get solar analysis for a job
     */
    public SolarAnalysis getAnalysisByJobId(Long jobId) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found"));

        return solarAnalysisRepository.findByJob(job)
            .orElse(null);
    }

    /**
     * Update solar analysis
     */
    public SolarAnalysis updateAnalysis(Long id, SolarAnalysis updates) {
        SolarAnalysis existing = getAnalysisById(id);

        // Update fields if provided
        if (updates.getRoofArea() != null) existing.setRoofArea(updates.getRoofArea());
        if (updates.getSystemCapacity() != null) existing.setSystemCapacity(updates.getSystemCapacity());
        if (updates.getNumberOfPanels() != null) existing.setNumberOfPanels(updates.getNumberOfPanels());

        return solarAnalysisRepository.save(existing);
    }

    /**
     * Delete solar analysis
     */
    public void deleteAnalysis(Long id) {
        solarAnalysisRepository.deleteById(id);
        log.info("Deleted solar analysis: {}", id);
    }

    /**
     * Quick calculation without saving
     */
    public SolarAnalysis calculateOptimal(
        Double latitude,
        Double longitude,
        Double roofArea,
        Double targetCapacity
    ) {
        return performAnalysis(null, null, latitude, longitude, roofArea, targetCapacity, "tile", null);
    }
}
