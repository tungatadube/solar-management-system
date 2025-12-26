package com.solar.management.service;

import com.solar.management.entity.MaterialRequirements;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class MaterialCalculationService {

    // Panel specifications
    private static final String PANEL_TYPE = "330W Monocrystalline PERC";
    private static final String PANEL_DIMENSIONS = "1.7m x 1.0m";

    // Pricing (AUD)
    private static final BigDecimal PANEL_UNIT_PRICE = BigDecimal.valueOf(250);
    private static final BigDecimal INVERTER_PRICE_PER_KW = BigDecimal.valueOf(1500);
    private static final BigDecimal MOUNTING_PRICE_PER_PANEL = BigDecimal.valueOf(75);
    private static final BigDecimal ELECTRICAL_BASE_PRICE = BigDecimal.valueOf(500);
    private static final BigDecimal LABOR_PRICE_PER_KW = BigDecimal.valueOf(1500);

    /**
     * Calculate all materials needed for installation
     */
    public MaterialRequirements calculateMaterials(
        int numberOfPanels,
        double systemCapacity,
        String roofType,
        String installationType,
        int layoutRows,
        int layoutColumns,
        double panelSpacing
    ) {
        log.info("Calculating materials for {} panels ({}x{}), {}kW system",
                 numberOfPanels, layoutRows, layoutColumns, systemCapacity);

        // Calculate inverter size (typically 110-120% of panel capacity)
        double inverterCapacity = Math.ceil(systemCapacity * 1.15);
        String inverterModel = selectInverterModel(inverterCapacity);

        // Calculate mounting hardware
        int railsQuantity = calculateRails(numberOfPanels);

        // Calculate optimized rail cuts
        RailCutOptimization railOptimization = calculateOptimizedRailCuts(layoutRows, layoutColumns, panelSpacing);

        int clampsQuantity = numberOfPanels * 4; // 4 clamps per panel
        int hooksQuantity = calculateHooks(numberOfPanels, roofType);
        int flashingsQuantity = hooksQuantity; // 1 flashing per hook

        // Calculate electrical components
        double dcCableLength = calculateDCCableLength(numberOfPanels);
        double acCableLength = 30.0; // Typical AC cable run
        double conduitLength = (dcCableLength + acCableLength) * 0.6; // 60% needs conduit
        int isolatorQuantity = 2; // DC and AC isolators
        int mcConnectors = numberOfPanels * 2; // 2 MC4 connectors per panel

        // Additional components
        int junctionBoxes = (int) Math.ceil(numberOfPanels / 10.0); // 1 per 10 panels
        int surgeProtectors = 2; // DC and AC surge protection
        int earthingKit = 1;

        // Calculate costs
        BigDecimal panelCost = PANEL_UNIT_PRICE.multiply(BigDecimal.valueOf(numberOfPanels));
        BigDecimal inverterCost = INVERTER_PRICE_PER_KW.multiply(BigDecimal.valueOf(inverterCapacity));
        BigDecimal mountingCost = MOUNTING_PRICE_PER_PANEL.multiply(BigDecimal.valueOf(numberOfPanels));
        BigDecimal electricalCost = ELECTRICAL_BASE_PRICE
            .add(BigDecimal.valueOf(dcCableLength * 5)) // $5 per meter
            .add(BigDecimal.valueOf(acCableLength * 3)); // $3 per meter
        BigDecimal laborCost = LABOR_PRICE_PER_KW.multiply(BigDecimal.valueOf(systemCapacity));
        BigDecimal totalCost = panelCost.add(inverterCost).add(mountingCost)
            .add(electricalCost).add(laborCost);

        // Estimate installation time
        int estimatedInstallDays = calculateInstallationDays(numberOfPanels);

        return MaterialRequirements.builder()
            // Panels
            .panelQuantity(numberOfPanels)
            .panelType(PANEL_TYPE)
            .panelDimensions(PANEL_DIMENSIONS)
            // Inverter
            .inverterQuantity(1)
            .inverterType(inverterCapacity <= 5 ? "Single Phase" : "Three Phase")
            .inverterModel(inverterModel)
            .inverterCapacity(inverterCapacity)
            // Mounting
            .railsQuantity(railsQuantity)
            .rails4m(railOptimization.rails4m)
            .rails6m(railOptimization.rails6m)
            .railCutPlan(railOptimization.getCutPlanJson())
            .railWastage(railOptimization.totalWastage)
            .clampsQuantity(clampsQuantity)
            .hooksQuantity(hooksQuantity)
            .flashingsQuantity(flashingsQuantity)
            // Electrical
            .dcCableLength(dcCableLength)
            .acCableLength(acCableLength)
            .conduitLength(conduitLength)
            .isolatorQuantity(isolatorQuantity)
            .mcConnectors(mcConnectors)
            // Additional
            .junctionBoxes(junctionBoxes)
            .surgeProtectors(surgeProtectors)
            .earthingKit(earthingKit)
            // Costs
            .panelCost(panelCost.setScale(2, RoundingMode.HALF_UP))
            .inverterCost(inverterCost.setScale(2, RoundingMode.HALF_UP))
            .mountingCost(mountingCost.setScale(2, RoundingMode.HALF_UP))
            .electricalCost(electricalCost.setScale(2, RoundingMode.HALF_UP))
            .laborCost(laborCost.setScale(2, RoundingMode.HALF_UP))
            .totalCost(totalCost.setScale(2, RoundingMode.HALF_UP))
            // Installation
            .roofType(roofType)
            .installationType(installationType)
            .estimatedInstallDays(estimatedInstallDays)
            .build();
    }

    /**
     * Calculate number of rails needed
     */
    private int calculateRails(int numberOfPanels) {
        // Typically 2 rails per panel (horizontal mounting)
        return numberOfPanels * 2;
    }

    /**
     * Calculate number of roof hooks based on roof type
     */
    private int calculateHooks(int numberOfPanels, String roofType) {
        return switch (roofType.toLowerCase()) {
            case "tile" -> numberOfPanels * 3; // 3 hooks per panel for tile
            case "metal" -> numberOfPanels * 2; // 2 hooks per panel for metal
            case "flat" -> 0; // No hooks for flat roof (uses ballast)
            default -> numberOfPanels * 3;
        };
    }

    /**
     * Calculate DC cable length based on array size
     */
    private double calculateDCCableLength(int numberOfPanels) {
        // Estimate based on panel count
        // Includes string wiring and runs to inverter
        return numberOfPanels * 5.0 + 20.0; // 5m per panel + 20m to inverter
    }

    /**
     * Select appropriate inverter model based on capacity
     */
    private String selectInverterModel(double capacity) {
        if (capacity <= 3) {
            return "Fronius Primo 3.0";
        } else if (capacity <= 5) {
            return "Fronius Primo 5.0";
        } else if (capacity <= 8) {
            return "Fronius Primo 8.2";
        } else if (capacity <= 10) {
            return "Fronius Symo 10.0";
        } else {
            return "Fronius Symo " + Math.ceil(capacity);
        }
    }

    /**
     * Estimate installation days based on system size
     */
    private int calculateInstallationDays(int numberOfPanels) {
        if (numberOfPanels <= 10) {
            return 1; // Small system: 1 day
        } else if (numberOfPanels <= 20) {
            return 2; // Medium system: 2 days
        } else if (numberOfPanels <= 30) {
            return 3; // Large system: 3 days
        } else {
            return 4; // Very large: 4+ days
        }
    }

    /**
     * Calculate material requirements for battery storage addon
     */
    public MaterialRequirements calculateBatteryMaterials(double batteryCapacity) {
        // Battery system materials
        BigDecimal batteryCost = BigDecimal.valueOf(batteryCapacity * 1200); // $1200 per kWh
        BigDecimal installationCost = BigDecimal.valueOf(2000); // Base installation

        return MaterialRequirements.builder()
            .totalCost(batteryCost.add(installationCost))
            .estimatedInstallDays(1)
            .build();
    }

    /**
     * Calculate optimized rail cuts for 4m and 6m standard sections
     * Uses First-Fit Decreasing bin packing algorithm to minimize waste
     */
    private RailCutOptimization calculateOptimizedRailCuts(int layoutRows, int layoutColumns, double panelSpacing) {
        final double PANEL_WIDTH = 1.0;  // meters
        final double PANEL_HEIGHT = 1.7; // meters
        final double RAIL_4M = 4.0;
        final double RAIL_6M = 6.0;
        final double MIN_USABLE = 0.5; // Cuts below this are waste

        RailCutOptimization result = new RailCutOptimization();
        java.util.List<Double> requiredLengths = new java.util.ArrayList<>();

        // Calculate horizontal rail lengths (2 per row)
        double horizontalLength = layoutColumns * PANEL_WIDTH + (layoutColumns - 1) * panelSpacing;
        for (int i = 0; i < layoutRows * 2; i++) {
            requiredLengths.add(horizontalLength);
        }

        // Calculate vertical rail lengths (2 per column) - for edge support
        double verticalLength = layoutRows * PANEL_HEIGHT + (layoutRows - 1) * panelSpacing;
        for (int i = 0; i < layoutColumns * 2; i++) {
            requiredLengths.add(verticalLength);
        }

        // Sort descending (First-Fit Decreasing)
        requiredLengths.sort(java.util.Collections.reverseOrder());

        // Bin packing algorithm
        java.util.List<Rail> rails6m = new java.util.ArrayList<>();
        java.util.List<Rail> rails4m = new java.util.ArrayList<>();

        for (Double length : requiredLengths) {
            boolean placed = false;

            // Try to fit in existing 6m rails first
            for (Rail rail : rails6m) {
                if (rail.remainingLength >= length + 0.01) { // 1cm tolerance
                    rail.addCut(length);
                    placed = true;
                    break;
                }
            }

            // Try existing 4m rails
            if (!placed && length <= RAIL_4M) {
                for (Rail rail : rails4m) {
                    if (rail.remainingLength >= length + 0.01) {
                        rail.addCut(length);
                        placed = true;
                        break;
                    }
                }
            }

            // Create new rail
            if (!placed) {
                if (length <= RAIL_4M) {
                    Rail newRail = new Rail(RAIL_4M, "4m");
                    newRail.addCut(length);
                    rails4m.add(newRail);
                } else {
                    Rail newRail = new Rail(RAIL_6M, "6m");
                    newRail.addCut(length);
                    rails6m.add(newRail);
                }
            }
        }

        // Aggregate cuts and calculate wastage
        java.util.Map<String, Integer> cutCounts = new java.util.HashMap<>();

        for (Rail rail : rails6m) {
            for (Double cut : rail.cuts) {
                String key = String.format("%.2f_6m", cut);
                cutCounts.put(key, cutCounts.getOrDefault(key, 0) + 1);
            }
            if (rail.remainingLength < MIN_USABLE) {
                result.totalWastage += rail.remainingLength;
            }
        }

        for (Rail rail : rails4m) {
            for (Double cut : rail.cuts) {
                String key = String.format("%.2f_4m", cut);
                cutCounts.put(key, cutCounts.getOrDefault(key, 0) + 1);
            }
            if (rail.remainingLength < MIN_USABLE) {
                result.totalWastage += rail.remainingLength;
            }
        }

        result.rails4m = rails4m.size();
        result.rails6m = rails6m.size();

        // Convert to RailCut objects
        cutCounts.forEach((key, count) -> {
            String[] parts = key.split("_");
            RailCut cut = new RailCut();
            cut.length = Double.parseDouble(parts[0]);
            cut.count = count;
            cut.source = parts[1];
            cut.purpose = determinePurpose(cut.length, horizontalLength, verticalLength);
            result.cuts.add(cut);
        });

        return result;
    }

    private String determinePurpose(double length, double horizLen, double vertLen) {
        if (Math.abs(length - horizLen) < 0.01) {
            return "Horizontal support rail";
        } else if (Math.abs(length - vertLen) < 0.01) {
            return "Vertical support rail";
        }
        return "Support rail";
    }

    /**
     * Helper class for rail cut optimization result
     */
    private static class RailCutOptimization {
        java.util.List<RailCut> cuts = new java.util.ArrayList<>();
        int rails4m = 0;
        int rails6m = 0;
        double totalWastage = 0.0;

        String getCutPlanJson() {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.writeValueAsString(cuts);
            } catch (Exception e) {
                log.error("Failed to serialize rail cut plan", e);
                return "[]";
            }
        }
    }

    /**
     * Helper class for individual rail cut
     */
    private static class RailCut {
        public double length;
        public int count;
        public String source; // "4m" or "6m"
        public String purpose;
    }

    /**
     * Helper class for bin packing algorithm
     */
    private static class Rail {
        double totalLength;
        double remainingLength;
        String type;
        java.util.List<Double> cuts = new java.util.ArrayList<>();

        Rail(double length, String type) {
            this.totalLength = length;
            this.remainingLength = length;
            this.type = type;
        }

        void addCut(double cutLength) {
            cuts.add(cutLength);
            remainingLength -= cutLength;
        }
    }
}
