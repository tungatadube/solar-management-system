# Solar Installation Management System

A comprehensive full-stack application for managing solar installation operations, including stock management, job tracking, GPS location services, and reporting.

## Features

### 🔧 Core Functionality
- **Job Management**: Create, track, and manage solar installation jobs from scheduling to completion
- **Stock Management**: Track inventory across multiple locations (warehouse, vehicles, job sites)
- **Location Tracking**: Real-time GPS tracking of technicians with route history
- **Image Documentation**: Upload and organize job photos with GPS coordinates
- **Travel Logging**: Automatic distance and duration calculation for job site visits
- **Reporting**: Generate comprehensive reports on jobs, stock usage, and travel

### 📊 Key Features
- Real-time location tracking with live map view
- Stock level alerts and low inventory notifications
- Multi-location stock management (warehouse, vehicle, site)
- Job status workflow (Scheduled → In Progress → Completed)
- Photo documentation with categorization
- Distance and travel time calculation
- Role-based access control (Admin, Manager, Technician, Assistant)
- Responsive Material-UI interface

## Technology Stack

### Backend
- **Java 17** with **Spring Boot 3.2**
- **Spring Data JPA** for database operations
- **Spring Security** with JWT authentication
- **PostgreSQL** database
- **Maven** for dependency management

### Frontend
- **React 18** with **TypeScript**
- **Material-UI (MUI)** for UI components
- **React Router** for navigation
- **React Query** for server state management
- **Leaflet** for map integration
- **Chart.js** for data visualization

## Prerequisites

- **Java 17+**
- **Node.js 18+** and **npm**
- **PostgreSQL 15+**
- **Maven 3.8+**
- **Docker** and **Docker Compose** (optional)

## Quick Start with Docker

The easiest way to run the entire system:

```bash
# Clone the repository
git clone <repository-url>
cd solar-management-system

# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f
```

The application will be available at:
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Database: localhost:5432

## Manual Setup

### 1. Database Setup

```bash
# Create PostgreSQL database
createdb solar_management

# Or using psql
psql -U postgres
CREATE DATABASE solar_management;
```

### 2. Backend Setup

```bash
cd backend

# Configure application.yml with your database credentials
# Edit src/main/resources/application.yml

# Build and run
mvn clean install
mvn spring-boot:run

# Or using Maven wrapper
./mvnw spring-boot:run
```

The backend will start on http://localhost:8080

### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm start
```

The frontend will start on http://localhost:3000

## Database Schema

The system includes the following main entities:

- **Users**: User accounts with roles (Admin, Manager, Technician, Assistant)
- **Jobs**: Solar installation jobs with status tracking
- **Locations**: Physical locations (warehouses, vehicles, job sites)
- **StockItems**: Inventory items with categories
- **StockLocations**: Stock quantities at each location
- **StockMovements**: History of stock transfers
- **JobStock**: Stock items used in jobs
- **JobImages**: Photos uploaded for jobs
- **TravelLogs**: Travel records between locations
- **LocationTracking**: GPS coordinates over time

## API Endpoints

### Jobs
```
GET    /api/jobs              - Get all jobs
GET    /api/jobs/{id}         - Get job by ID
POST   /api/jobs              - Create new job
PUT    /api/jobs/{id}         - Update job
PATCH  /api/jobs/{id}/status  - Update job status
DELETE /api/jobs/{id}         - Delete job

GET    /api/jobs/{id}/images  - Get job images
POST   /api/jobs/{id}/images  - Upload job image
GET    /api/jobs/{id}/travel  - Get travel logs
POST   /api/jobs/{id}/travel  - Log travel
```

### Location Tracking
```
POST   /api/location-tracking                        - Record location
GET    /api/location-tracking/user/{id}/latest      - Get latest location
GET    /api/location-tracking/user/{id}/history     - Get location history
GET    /api/location-tracking/distance              - Calculate distance
```

### Stock Management
```
GET    /api/stock              - Get all stock items
GET    /api/stock/{id}         - Get stock item
POST   /api/stock              - Create stock item
PUT    /api/stock/{id}         - Update stock item
GET    /api/stock/low-stock    - Get low stock alerts
```

## Configuration

### Backend Configuration (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/solar_management
    username: postgres
    password: your_password

jwt:
  secret: your-secret-key
  expiration: 86400000

file:
  upload-dir: ./uploads

# For AWS S3 (optional)
aws:
  s3:
    bucket-name: your-bucket
    region: ap-southeast-2
```

### Frontend Configuration (.env)

```
REACT_APP_API_URL=http://localhost:8080/api
```

## User Roles and Permissions

| Role | Permissions |
|------|------------|
| **Admin** | Full system access, user management, system configuration |
| **Manager** | Job management, reporting, stock oversight |
| **Technician** | Job execution, location tracking, image upload |
| **Assistant** | Limited job access, stock viewing |

## Mobile Integration

The system is designed to work with mobile devices for field technicians:

### Location Tracking
```javascript
// Example: Record location from mobile browser
navigator.geolocation.getCurrentPosition((position) => {
  fetch('http://your-server/api/location-tracking/user/1', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      latitude: position.coords.latitude,
      longitude: position.coords.longitude,
      accuracy: position.coords.accuracy,
      timestamp: new Date().toISOString()
    })
  });
});
```

### Image Upload
```javascript
// Example: Upload job image with GPS coordinates
const formData = new FormData();
formData.append('file', imageFile);
formData.append('imageType', 'DURING_INSTALLATION');
formData.append('latitude', lat);
formData.append('longitude', lng);

fetch('http://your-server/api/jobs/123/images', {
  method: 'POST',
  body: formData
});
```

## Reporting Features

The system can generate:
- **Job Completion Reports**: Details of completed jobs with photos and stock usage
- **Stock Usage Reports**: Track stock consumption across jobs
- **Travel Reports**: Distance, duration, and fuel costs per technician
- **Performance Metrics**: Job completion times, efficiency metrics
- **Cost Analysis**: Labor, stock, and travel costs per job

## Development

### Adding New Features

1. **Backend**: Add entities, repositories, services, and controllers
2. **Frontend**: Create components and API service methods
3. **Database**: Schema updates handled automatically by JPA

### Testing

```bash
# Backend tests
cd backend
mvn test

# Frontend tests
cd frontend
npm test
```

## Production Deployment

### Backend Deployment

```bash
# Build production JAR
mvn clean package -DskipTests

# Run with production profile
java -jar target/management-system-1.0.0.jar --spring.profiles.active=prod
```

### Frontend Deployment

```bash
# Build production bundle
npm run build

# Deploy the build folder to your web server
```

### Security Considerations

- Change default JWT secret in production
- Use HTTPS for all communications
- Implement rate limiting
- Regular security audits
- Secure database credentials
- Enable CORS only for trusted domains

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check PostgreSQL is running
   - Verify credentials in application.yml
   - Ensure database exists

2. **Port Already in Use**
   - Backend: Change `server.port` in application.yml
   - Frontend: Run `PORT=3001 npm start`

3. **CORS Errors**
   - Update `app.cors.allowed-origins` in application.yml
   - Ensure frontend URL is whitelisted

## Future Enhancements

- [ ] Mobile native apps (iOS/Android)
- [ ] Push notifications for job updates
- [ ] Advanced route optimization
- [ ] Integration with accounting software
- [ ] Customer portal for job status
- [ ] Predictive maintenance scheduling
- [ ] AI-powered stock forecasting

## Support

For issues and questions:
- Create an issue in the repository
- Email: support@solarmanagement.com
- Documentation: https://docs.solarmanagement.com

## License

Proprietary - All rights reserved

## Contributors

- Development Team
- Solar Installation Specialists
- UI/UX Designers

---

Built with ❤️ for solar installation professionals
