# Solar Installation Management System

## Project Overview
A comprehensive system for managing solar installation operations including stock management, job tracking, location services, and reporting.

## Technology Stack

### Backend
- Java 17+
- Spring Boot 3.2+
- Spring Data JPA
- Spring Security (JWT)
- PostgreSQL
- AWS S3 / Local Storage (for images)
- Lombok
- MapStruct (for DTOs)

### Frontend
- React 18+
- TypeScript
- Material-UI (MUI)
- React Router
- Axios
- React Query
- Leaflet (for maps)
- Chart.js (for reports)

## Project Structure

```
solar-management-system/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/solar/management/
│   │   │   │   ├── config/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   ├── security/
│   │   │   │   ├── exception/
│   │   │   │   └── util/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── application-prod.yml
│   │   └── test/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   ├── hooks/
│   │   ├── utils/
│   │   ├── types/
│   │   └── App.tsx
│   ├── package.json
│   └── Dockerfile
└── docker-compose.yml
```

## Core Features

### 1. Stock Management
- Track inventory levels
- Stock locations (warehouse, vehicle, site)
- Stock movements and transfers
- Low stock alerts
- Barcode/QR code support

### 2. Job Management
- Job creation and assignment
- Job status tracking
- Time tracking
- Photo documentation
- Digital signatures

### 3. Location Services
- Real-time GPS tracking
- Route optimization
- Travel time calculation
- Geofencing for sites
- Distance reporting

### 4. Reporting
- Job completion reports
- Stock usage reports
- Travel time and distance reports
- Cost analysis
- Performance metrics
- Export to PDF/Excel

### 5. User Management
- Role-based access control (Admin, Manager, Technician)
- User authentication (JWT)
- Activity logging
