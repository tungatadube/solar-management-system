package com.solar.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for solar panel calculations including:
 * - Optimal panel orientation (azimuth and tilt)
 * - Production estimates
 * - Panel layout optimization
 */
@Service
@Slf4j
public class SolarCalculationService {

    private static final double STANDARD_PANEL_WATTAGE = 330.0; // Watts
    private static final double PANEL_WIDTH = 1.0; // meters
    private static final double PANEL_HEIGHT = 1.7; // meters
    private static final double PANEL_AREA = PANEL_WIDTH * PANEL_HEIGHT; // 1.7 m²
    private static final double SYSTEM_EFFICIENCY = 0.85; // 85% system efficiency
    private static final double PANEL_SPACING = 0.05; // 5cm spacing between panels

    /**
     * Calculate optimal azimuth angle (compass direction) for panels
     * @param latitude Location latitude
     * @return Optimal azimuth in degrees (0-360, where 0=North, 90=East, 180=South, 270=West)
     */
    public double calculateOptimalAzimuth(double latitude) {
        if (latitude < 0) {
            // Southern hemisphere - face North
            return 0.0;
        } else {
            // Northern hemisphere - face South
            return 180.0;
        }
    }

    /**
     * Calculate optimal tilt angle for panels based on latitude
     * @param latitude Location latitude
     * @return Optimal tilt angle in degrees from horizontal
     */
    public double calculateOptimalTilt(double latitude) {
        // Rule of thumb: tilt angle ≈ latitude for year-round optimization
        // For summer optimization: latitude - 15°
        // For winter optimization: latitude + 15°
        return Math.abs(latitude);
    }

    /**
     * Calculate number of panels needed for target capacity
     * @param targetCapacity Target system capacity in kW
     * @param panelWattage Individual panel wattage
     * @return Number of panels needed
     */
    public int calculatePanelQuantity(double targetCapacity, double panelWattage) {
        double targetWatts = targetCapacity * 1000;
        return (int) Math.ceil(targetWatts / panelWattage);
    }

    /**
     * Calculate peak sun hours for a location based on latitude
     * This is a simplified calculation - real-world would use weather data
     * @param latitude Location latitude
     * @return Average daily peak sun hours
     */
    public double calculatePeakSunHours(double latitude) {
        double absLatitude = Math.abs(latitude);

        // Simplified model: peak sun hours decrease with higher latitude
        // Equator (0°): ~6 hours
        // Mid-latitudes (30-40°): ~5 hours
        // Higher latitudes (60°+): ~3 hours

        if (absLatitude < 10) {
            return 6.0;
        } else if (absLatitude < 25) {
            return 5.5;
        } else if (absLatitude < 40) {
            return 5.0;
        } else if (absLatitude < 50) {
            return 4.5;
        } else {
            return 4.0;
        }
    }

    /**
     * Estimate annual energy production
     * @param numberOfPanels Number of solar panels
     * @param panelWattage Wattage per panel
     * @param peakSunHours Average daily peak sun hours
     * @param systemEfficiency System efficiency (0.0 to 1.0)
     * @return Estimated annual production in kWh
     */
    public double estimateAnnualProduction(
        int numberOfPanels,
        double panelWattage,
        double peakSunHours,
        double systemEfficiency
    ) {
        // Daily production = panels × wattage × peak sun hours × efficiency / 1000
        double dailyProduction = numberOfPanels * panelWattage * peakSunHours * systemEfficiency / 1000.0;

        // Annual production
        return dailyProduction * 365.0;
    }

    /**
     * Calculate maximum number of panels that can fit on roof
     * @param roofArea Total roof area in m²
     * @param usablePercentage Percentage of roof area that's usable (0.0 to 1.0)
     * @return Maximum number of panels that fit
     */
    public int calculateMaxPanels(double roofArea, double usablePercentage) {
        double usableArea = roofArea * usablePercentage;

        // Account for spacing between panels
        double effectiveAreaPerPanel = PANEL_AREA + (PANEL_SPACING * 2 * (PANEL_WIDTH + PANEL_HEIGHT));

        return (int) Math.floor(usableArea / effectiveAreaPerPanel);
    }

    /**
     * Optimize panel layout for given roof dimensions
     * @param roofWidth Roof width in meters
     * @param roofLength Roof length in meters
     * @param numberOfPanels Number of panels to fit
     * @return Array [rows, columns] for optimal layout
     */
    public int[] optimizePanelLayout(double roofWidth, double roofLength, int numberOfPanels) {
        int bestRows = 1;
        int bestCols = numberOfPanels;
        double bestWastedSpace = Double.MAX_VALUE;

        // Try different row/column combinations
        for (int rows = 1; rows <= numberOfPanels; rows++) {
            int cols = (int) Math.ceil((double) numberOfPanels / rows);

            // Check if this layout fits
            double requiredWidth = cols * (PANEL_WIDTH + PANEL_SPACING);
            double requiredLength = rows * (PANEL_HEIGHT + PANEL_SPACING);

            if (requiredWidth <= roofWidth && requiredLength <= roofLength) {
                double wastedSpace = (roofWidth * roofLength) - (numberOfPanels * PANEL_AREA);

                if (wastedSpace < bestWastedSpace) {
                    bestWastedSpace = wastedSpace;
                    bestRows = rows;
                    bestCols = cols;
                }
            }
        }

        return new int[]{bestRows, bestCols};
    }

    /**
     * Calculate shading factor based on nearby obstructions
     * This is a placeholder - real implementation would use Google Maps data
     * @param latitude Location latitude
     * @param longitude Location longitude
     * @return Shading factor (0.0 = fully shaded, 1.0 = no shade)
     */
    public double calculateShadingFactor(double latitude, double longitude) {
        // Simplified: assume minimal shading
        // Real implementation would analyze:
        // - Nearby buildings
        // - Trees
        // - Terrain
        // - Sun path throughout the year
        return 0.85; // 85% sun exposure (15% shading)
    }

    /**
     * Calculate actual production considering shading
     * @param theoreticalProduction Theoretical annual production
     * @param shadingFactor Shading factor (0.0 to 1.0)
     * @return Actual estimated production
     */
    public double calculateActualProduction(double theoreticalProduction, double shadingFactor) {
        return theoreticalProduction * shadingFactor;
    }

    /**
     * Estimate system cost based on capacity
     * @param systemCapacity System capacity in kW
     * @param numberOfPanels Number of panels
     * @return Estimated total cost in AUD
     */
    public BigDecimal estimateSystemCost(double systemCapacity, int numberOfPanels) {
        // Cost breakdown (approximate AUD prices as of 2024):
        // - Panels: $200-300 per panel
        // - Inverter: $1000-2000 per kW
        // - Mounting: $50-100 per panel
        // - Installation: $1000-2000 per kW

        BigDecimal panelCost = BigDecimal.valueOf(numberOfPanels * 250); // $250 per panel
        BigDecimal inverterCost = BigDecimal.valueOf(systemCapacity * 1500); // $1500 per kW
        BigDecimal mountingCost = BigDecimal.valueOf(numberOfPanels * 75); // $75 per panel
        BigDecimal installationCost = BigDecimal.valueOf(systemCapacity * 1500); // $1500 per kW

        return panelCost
            .add(inverterCost)
            .add(mountingCost)
            .add(installationCost)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate roof pitch from Google Maps 3D data
     * This is a placeholder - real implementation would use actual elevation data
     * @param roofType Type of roof (tile, metal, flat)
     * @return Estimated roof pitch in degrees
     */
    public double estimateRoofPitch(String roofType) {
        return switch (roofType.toLowerCase()) {
            case "flat" -> 5.0; // Flat roofs typically have small pitch for drainage
            case "tile" -> 22.5; // Standard tile roof pitch
            case "metal" -> 20.0; // Metal roofs can have lower pitch
            default -> 20.0;
        };
    }

    /**
     * Determine roof orientation from azimuth
     * @param azimuth Roof azimuth in degrees
     * @return Readable orientation (N, NE, E, SE, S, SW, W, NW)
     */
    public String getOrientationFromAzimuth(double azimuth) {
        azimuth = azimuth % 360; // Normalize to 0-360

        if (azimuth >= 337.5 || azimuth < 22.5) return "North";
        if (azimuth < 67.5) return "North-East";
        if (azimuth < 112.5) return "East";
        if (azimuth < 157.5) return "South-East";
        if (azimuth < 202.5) return "South";
        if (azimuth < 247.5) return "South-West";
        if (azimuth < 292.5) return "West";
        return "North-West";
    }

    /**
     * Calculate daily average production
     * @param annualProduction Annual production in kWh
     * @return Daily average in kWh
     */
    public double calculateDailyAverage(double annualProduction) {
        return annualProduction / 365.0;
    }
}
