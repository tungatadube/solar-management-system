package com.solar.management.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "stock_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StockItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String sku;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockCategory category;
    
    @Column(nullable = false)
    private String unit; // e.g., "piece", "meter", "kg"
    
    @Column(nullable = false)
    private BigDecimal unitPrice;
    
    @Column(nullable = false)
    private Integer minimumQuantity = 10;
    
    @Column(nullable = false)
    private Integer reorderLevel = 20;
    
    @Column
    private String barcode;
    
    @Column
    private String imageUrl;
    
    @OneToMany(mappedBy = "stockItem", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<StockLocation> stockLocations = new HashSet<>();

    @OneToMany(mappedBy = "stockItem", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<StockMovement> stockMovements = new HashSet<>();

    @OneToMany(mappedBy = "stockItem", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<JobStock> jobStocks = new HashSet<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum StockCategory {
        SOLAR_PANEL,
        INVERTER,
        BATTERY,
        MOUNTING_HARDWARE,
        ELECTRICAL_COMPONENTS,
        CABLES_WIRING,
        TOOLS,
        SAFETY_EQUIPMENT,
        CONSUMABLES,
        OTHER
    }
    
    @Transient
    public Integer getTotalQuantity() {
        return stockLocations.stream()
                .mapToInt(StockLocation::getQuantity)
                .sum();
    }
}
