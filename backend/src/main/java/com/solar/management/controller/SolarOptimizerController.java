package com.solar.management.controller;

import com.solar.management.entity.SolarAnalysis;
import com.solar.management.service.SolarOptimizerService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solar-optimizer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SolarOptimizerController {

    private final SolarOptimizerService solarOptimizerService;

    /**
     * Perform complete solar analysis
     */
    @PostMapping("/analyze")
    public ResponseEntity<SolarAnalysis> performAnalysis(@RequestBody AnalysisRequest request) {
        try {
            SolarAnalysis analysis = solarOptimizerService.performAnalysis(
                request.getJobId(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRoofArea(),
                request.getTargetCapacity(),
                request.getRoofType()
            );
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Quick calculation without saving
     */
    @PostMapping("/calculate")
    public ResponseEntity<SolarAnalysis> calculateOptimal(@RequestBody QuickCalculationRequest request) {
        try {
            SolarAnalysis analysis = solarOptimizerService.calculateOptimal(
                request.getLatitude(),
                request.getLongitude(),
                request.getRoofArea(),
                request.getTargetCapacity()
            );
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get solar analysis by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SolarAnalysis> getAnalysisById(@PathVariable Long id) {
        try {
            SolarAnalysis analysis = solarOptimizerService.getAnalysisById(id);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get solar analysis for a job
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<SolarAnalysis> getAnalysisByJobId(@PathVariable Long jobId) {
        try {
            SolarAnalysis analysis = solarOptimizerService.getAnalysisByJobId(jobId);
            if (analysis != null) {
                return ResponseEntity.ok(analysis);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update solar analysis
     */
    @PutMapping("/{id}")
    public ResponseEntity<SolarAnalysis> updateAnalysis(
        @PathVariable Long id,
        @RequestBody SolarAnalysis updates
    ) {
        try {
            SolarAnalysis updated = solarOptimizerService.updateAnalysis(id, updates);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Delete solar analysis
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Long id) {
        try {
            solarOptimizerService.deleteAnalysis(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Request DTOs

    @Data
    public static class AnalysisRequest {
        private Long jobId;
        private String address;
        private Double latitude;
        private Double longitude;
        private Double roofArea;
        private Double targetCapacity;
        private String roofType; // tile, metal, flat
    }

    @Data
    public static class QuickCalculationRequest {
        private Double latitude;
        private Double longitude;
        private Double roofArea;
        private Double targetCapacity;
    }
}
