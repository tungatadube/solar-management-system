package com.solar.management.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequirements implements Serializable {

    // Panels
    private Integer panelQuantity;
    private String panelType;
    private String panelDimensions; // e.g., "1.7m x 1.0m"

    // Inverter
    private Integer inverterQuantity;
    private String inverterType;
    private String inverterModel;
    private Double inverterCapacity; // kW

    // Mounting hardware
    private Integer railsQuantity; // meters of rail
    private Integer rails4m; // Number of 4m rails needed
    private Integer rails6m; // Number of 6m rails needed
    private String railCutPlan; // JSON: [{length, count, source, purpose}, ...]
    private Double railWastage; // Total meters wasted
    private Integer clampsQuantity;
    private Integer hooksQuantity;
    private Integer flashingsQuantity;

    // Electrical components
    private Double dcCableLength; // meters
    private Double acCableLength; // meters
    private Double conduitLength; // meters
    private Integer isolatorQuantity;
    private Integer mcConnectors; // MC4 connectors

    // Additional components
    private Integer junctionBoxes;
    private Integer surgeProtectors;
    private Integer earthingKit;

    // Cost breakdown
    private BigDecimal panelCost;
    private BigDecimal inverterCost;
    private BigDecimal mountingCost;
    private BigDecimal electricalCost;
    private BigDecimal laborCost;
    private BigDecimal totalCost;

    // Installation details
    private String roofType; // tile, metal, flat
    private String installationType; // flush-mount, tilt-frame, ground-mount
    private Integer estimatedInstallDays;
}
