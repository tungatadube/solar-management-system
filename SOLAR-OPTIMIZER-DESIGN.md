# Solar Panel Optimization Microservice - Design Document

## Overview

A microservice that calculates optimal solar panel placement, orientation, and material requirements based on:
- Geographic location (lat/long)
- Roof dimensions from Google Maps
- Sun exposure patterns
- System requirements (kW capacity)

## Features

1. **Roof Measurement**
   - Integrate with Google Maps API to measure roof dimensions
   - Calculate usable roof area
   - Detect roof pitch/slope

2. **Optimal Direction Calculation**
   - Calculate optimal azimuth (compass direction)
   - Calculate optimal tilt angle
   - Consider seasonal sun paths
   - Account for shading and obstructions

3. **Panel Layout Optimization**
   - Calculate number of panels that fit
   - Optimize panel arrangement
   - Maximize sun exposure
   - Consider roof constraints

4. **Material Calculation**
   - Calculate number of panels needed
   - Calculate mounting hardware
   - Calculate wiring requirements
   - Calculate inverter specifications

## Technology Stack

- **Language**: Java Spring Boot (separate microservice)
- **Database**: PostgreSQL (shared or separate)
- **APIs**:
  - Google Maps API (Distance Matrix, Places)
  - Google Solar API (sun exposure data)
  - PVWatts API (NREL - solar calculations)
- **Calculations**: Custom solar algorithms

## API Endpoints

### 1. Roof Analysis
```
POST /api/solar-optimizer/analyze-roof
{
  "address": "123 Main St, Adelaide SA 5000",
  "latitude": -34.9285,
  "longitude": 138.6007
}

Response:
{
  "roofArea": 150.5,
  "usableArea": 120.0,
  "roofPitch": 22.5,
  "orientation": "North-facing",
  "shadingFactor": 0.85
}
```

### 2. Optimal Configuration
```
POST /api/solar-optimizer/calculate-optimal
{
  "latitude": -34.9285,
  "longitude": 138.6007,
  "roofArea": 120.0,
  "targetCapacity": 6.6,
  "roofOrientation": "North"
}

Response:
{
  "optimalAzimuth": 0,
  "optimalTilt": 30,
  "numberOfPanels": 20,
  "systemCapacity": 6.6,
  "estimatedAnnualProduction": 9500,
  "panelLayout": {
    "rows": 4,
    "columns": 5,
    "spacing": 0.05
  }
}
```

### 3. Material Requirements
```
POST /api/solar-optimizer/calculate-materials
{
  "numberOfPanels": 20,
  "systemCapacity": 6.6,
  "roofType": "tile",
  "installationType": "flush-mount"
}

Response:
{
  "panels": {
    "quantity": 20,
    "type": "330W Mono-PERC",
    "dimensions": "1.7m x 1.0m"
  },
  "inverter": {
    "quantity": 1,
    "type": "7.5kW Hybrid",
    "model": "Fronius Primo"
  },
  "mounting": {
    "rails": 40,
    "clamps": 80,
    "hooks": 60,
    "flashings": 60
  },
  "electrical": {
    "dcCable": 150,
    "acCable": 30,
    "conduit": 50,
    "isolator": 2
  },
  "estimatedCost": 8500
}
```

### 4. Sun Exposure Analysis
```
POST /api/solar-optimizer/sun-exposure
{
  "latitude": -34.9285,
  "longitude": 138.6007,
  "date": "2024-06-21"
}

Response:
{
  "sunrise": "07:15",
  "sunset": "17:08",
  "peakSunHours": 5.2,
  "optimalPanelAngle": 30,
  "seasonalVariation": {
    "summer": 7.5,
    "winter": 3.5,
    "spring": 5.5,
    "autumn": 5.0
  }
}
```

## Data Models

### SolarAnalysis Entity
```java
@Entity
public class SolarAnalysis {
    private Long id;
    private Long jobId;
    private String address;
    private Double latitude;
    private Double longitude;

    // Roof measurements
    private Double roofArea;
    private Double usableArea;
    private Double roofPitch;
    private String roofOrientation;

    // Optimal configuration
    private Double optimalAzimuth;
    private Double optimalTilt;
    private Integer numberOfPanels;
    private Double systemCapacity;

    // Production estimates
    private Double annualProduction;
    private Double dailyAverage;

    // Material requirements
    private MaterialRequirements materials;

    private LocalDateTime analyzedAt;
}
```

### MaterialRequirements Entity
```java
@Embeddable
public class MaterialRequirements {
    // Panels
    private Integer panelQuantity;
    private String panelType;

    // Inverter
    private Integer inverterQuantity;
    private String inverterType;

    // Mounting
    private Integer railsQuantity;
    private Integer clampsQuantity;
    private Integer hooksQuantity;

    // Electrical
    private Double dcCableLength;
    private Double acCableLength;

    // Cost
    private BigDecimal estimatedCost;
}
```

## Calculation Algorithms

### 1. Optimal Azimuth Calculation
For Southern Hemisphere (Australia):
- North-facing (0°) is optimal
- Adjust based on roof orientation
- Consider morning vs afternoon usage patterns

```java
public double calculateOptimalAzimuth(double latitude, String roofOrientation) {
    if (latitude < 0) { // Southern hemisphere
        return 0; // North
    } else { // Northern hemisphere
        return 180; // South
    }
}
```

### 2. Optimal Tilt Calculation
Based on latitude:
```java
public double calculateOptimalTilt(double latitude) {
    // Rule of thumb: latitude - 15° for summer
    // or latitude for year-round
    // or latitude + 15° for winter
    return Math.abs(latitude);
}
```

### 3. Panel Quantity Calculation
```java
public int calculatePanelQuantity(double targetCapacity, double panelWattage) {
    return (int) Math.ceil(targetCapacity * 1000 / panelWattage);
}
```

### 4. Annual Production Estimate
```java
public double estimateAnnualProduction(
    int numberOfPanels,
    double panelWattage,
    double peakSunHours,
    double systemEfficiency
) {
    double dailyProduction = numberOfPanels * panelWattage * peakSunHours * systemEfficiency / 1000;
    return dailyProduction * 365;
}
```

## Google Maps Integration

### API Key Requirements
- Google Maps JavaScript API
- Google Maps Geocoding API
- Google Distance Matrix API

### Roof Measurement Process
1. Geocode address to get coordinates
2. Load satellite imagery
3. Use polygon drawing tool to outline roof
4. Calculate area using geometry library
5. Detect roof pitch from 3D imagery (if available)

## Implementation Plan

### Phase 1: Core Service (Week 1)
- [ ] Create microservice project structure
- [ ] Implement basic entities and repositories
- [ ] Create calculation algorithms
- [ ] Build REST APIs

### Phase 2: Google Maps Integration (Week 2)
- [ ] Set up Google Maps API
- [ ] Implement roof measurement
- [ ] Add geocoding service
- [ ] Create frontend map interface

### Phase 3: Material Calculations (Week 3)
- [ ] Define material database
- [ ] Implement cost calculations
- [ ] Add material optimization logic
- [ ] Create material reports

### Phase 4: Advanced Features (Week 4)
- [ ] Sun exposure simulation
- [ ] 3D visualization
- [ ] Shading analysis
- [ ] Performance monitoring

## Database Schema

```sql
CREATE TABLE solar_analyses (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT REFERENCES jobs(id),
    address VARCHAR(500),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),

    -- Roof measurements
    roof_area DECIMAL(10, 2),
    usable_area DECIMAL(10, 2),
    roof_pitch DECIMAL(5, 2),
    roof_orientation VARCHAR(50),

    -- Optimal configuration
    optimal_azimuth DECIMAL(5, 2),
    optimal_tilt DECIMAL(5, 2),
    number_of_panels INT,
    system_capacity DECIMAL(10, 2),

    -- Production estimates
    annual_production DECIMAL(10, 2),
    daily_average DECIMAL(10, 2),

    -- Materials (JSON)
    materials JSONB,

    analyzed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE material_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    category VARCHAR(100), -- PANEL, INVERTER, MOUNTING, ELECTRICAL
    unit_price DECIMAL(10, 2),
    specifications JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_solar_analyses_job ON solar_analyses(job_id);
CREATE INDEX idx_solar_analyses_location ON solar_analyses(latitude, longitude);
```

## Frontend Components

### 1. Roof Measurement Tool
- Interactive Google Map
- Polygon drawing tool
- Area calculation display
- Satellite view

### 2. Optimization Dashboard
- System configuration form
- Optimal angle visualization
- Panel layout preview
- Production estimates

### 3. Material Calculator
- Auto-populated material list
- Quantity adjustments
- Cost breakdown
- Export to quote

## External APIs to Use

1. **Google Maps JavaScript API**
   - https://developers.google.com/maps/documentation/javascript

2. **NREL PVWatts API** (FREE)
   - https://developer.nrel.gov/docs/solar/pvwatts/
   - Accurate solar production estimates

3. **OpenWeatherMap Solar Radiation API**
   - Historical sun data
   - Cloud cover analysis

## Security Considerations

- Secure API keys in environment variables
- Rate limiting for external API calls
- Validate geographic coordinates
- Sanitize address inputs

## Testing Strategy

- Unit tests for calculations
- Integration tests for Google Maps API
- End-to-end tests for full workflow
- Mock external API responses

## Deployment

- Deploy as separate Docker container
- Add to docker-compose.yml
- Configure environment variables
- Set up API key management

## Cost Estimation

- Google Maps API: ~$0.005 per request
- NREL API: FREE (with API key)
- Compute: Minimal (simple calculations)

**Estimated**: ~$5-20/month for 1000 analyses

## Next Steps

1. Review and approve design
2. Set up Google Maps API account
3. Create microservice project structure
4. Implement core calculations
5. Build frontend interface

---

Let me know if you'd like me to proceed with implementation!
