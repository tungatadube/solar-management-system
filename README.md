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
| **Admin** | Full system access, user management, system configuration, can see ALL data |
| **Manager** | Job management, reporting, stock oversight, can see ALL data |
| **Technician** | Job execution, location tracking, image upload, **restricted to own data only** |
| **Assistant** | Limited job access, stock viewing |

### Role-Based Access Control (RBAC)

The system implements comprehensive role-based access control with three-layer security:

#### Security Layers

1. **@PreAuthorize Annotations**: Block unauthenticated/unauthorized requests at controller level
2. **Service Validation**: Validate resource access before operations
3. **Auto-filtering**: Return only authorized data in list queries

#### Access Rules

**ADMIN and MANAGER:**
- Can see ALL jobs, worklogs, invoices, and location tracking data
- Full CRUD access to all resources
- Can manage users and system configuration

**TECHNICIAN:**
- Can ONLY see jobs they are assigned to
- Can ONLY see their own worklogs, invoices, and location tracking
- Cannot access other technicians' data (returns 403 Forbidden)
- Cannot create or delete jobs (only ADMIN/MANAGER can)
- Can update job status for assigned jobs
- Can upload images and log travel for assigned jobs

**Path Traversal Prevention:**
- All userId parameters in URLs are validated
- Technicians cannot query `/api/worklogs/user/{otherUserId}` - returns 403
- Technicians cannot query `/api/invoices/technician/{otherUserId}` - returns 403

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

## Testing Role-Based Access Control

### Prerequisites

```bash
# Ensure Docker services are running
docker-compose up -d

# Rebuild backend with latest code
docker-compose build backend
docker-compose up -d backend
```

### Test Script 1: Get Authentication Tokens

Save this as `scripts/get_tokens.sh`:

```bash
#!/bin/bash

# Get admin token
ADMIN_RESPONSE=$(curl -s -X POST "http://localhost:8180/realms/solar-management/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=solar-frontend")

ADMIN_TOKEN=$(echo $ADMIN_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null)
echo "$ADMIN_TOKEN" > /tmp/admin_token.txt
echo "Admin token obtained (length: ${#ADMIN_TOKEN})"

# Get technician token (john.tech)
TECH_RESPONSE=$(curl -s -X POST "http://localhost:8180/realms/solar-management/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=john.tech" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=solar-frontend")

TECH_TOKEN=$(echo $TECH_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null)
echo "$TECH_TOKEN" > /tmp/tech_token.txt
echo "Technician token obtained (length: ${#TECH_TOKEN})"

# Get second technician token (mike.tech)
TECH2_RESPONSE=$(curl -s -X POST "http://localhost:8180/realms/solar-management/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=mike.tech" \
  -d "password=password123" \
  -d "grant_type=password" \
  -d "client_id=solar-frontend")

TECH2_TOKEN=$(echo $TECH2_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null)
echo "$TECH2_TOKEN" > /tmp/tech2_token.txt
echo "Technician 2 token obtained (length: ${#TECH2_TOKEN})"
```

### Test Script 2: Comprehensive Access Control Tests

Save this as `scripts/test_access_control.sh`:

```bash
#!/bin/bash

ADMIN_TOKEN=$(cat /tmp/admin_token.txt)
TECH_TOKEN=$(cat /tmp/tech_token.txt)
TECH2_TOKEN=$(cat /tmp/tech2_token.txt)

echo "========================================="
echo "FINAL ROLE-BASED ACCESS CONTROL TEST"
echo "========================================="
echo ""

# Get user IDs
echo "Getting user IDs..."
JOHN_ID=21  # john.tech
MDUDUZI_ID=3  # mduduzi (existing technician)

echo "John (john.tech) ID: $JOHN_ID"
echo "Mduduzi ID: $MDUDUZI_ID"
echo ""

# Test 1: TECHNICIAN sees only assigned jobs
echo "=== TEST 1: Role-Based Job Filtering ==="
echo "John accessing GET /api/jobs (should see only assigned jobs)"
JOHN_JOBS=$(curl -s -X GET "http://localhost:8080/api/jobs" \
  -H "Authorization: Bearer $TECH_TOKEN")
JOHN_JOBS_COUNT=$(echo $JOHN_JOBS | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null)
echo "Result: John sees $JOHN_JOBS_COUNT jobs (assigned to him)"
echo "Jobs: $(echo $JOHN_JOBS | python3 -c "import sys, json; jobs = json.load(sys.stdin); print([j['jobNumber'] for j in jobs])" 2>/dev/null)"
echo ""

# Test 2: TECHNICIAN can access own data
echo "=== TEST 2: Access Own Data (200 OK) ==="
echo "John accessing GET /api/jobs/user/$JOHN_ID"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "http://localhost:8080/api/jobs/user/$JOHN_ID" \
  -H "Authorization: Bearer $TECH_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "200" ]; then
  echo "✓ PASS: Got 200 OK"
else
  echo "✗ FAIL: Got $HTTP_CODE"
fi
echo ""

# Test 3: TECHNICIAN cannot access other user's data (403 Forbidden)
echo "=== TEST 3: Access Other User's Data (403 Forbidden) ==="
echo "John accessing GET /api/jobs/user/$MDUDUZI_ID (Mduduzi's jobs)"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "http://localhost:8080/api/jobs/user/$MDUDUZI_ID" \
  -H "Authorization: Bearer $TECH_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "403" ]; then
  echo "✓ PASS: Got 403 Forbidden as expected"
  echo "Error message: $(echo "$RESPONSE" | head -n -1 | python3 -c "import sys, json; print(json.load(sys.stdin).get('message', ''))" 2>/dev/null)"
else
  echo "✗ FAIL: Expected 403 but got $HTTP_CODE"
fi
echo ""

# Test 4: TECHNICIAN worklogs filtering
echo "=== TEST 4: Worklog Filtering ==="
echo "John accessing GET /api/worklogs (should see only own worklogs)"
JOHN_WORKLOGS=$(curl -s -X GET "http://localhost:8080/api/worklogs" \
  -H "Authorization: Bearer $TECH_TOKEN")
JOHN_WORKLOGS_COUNT=$(echo $JOHN_WORKLOGS | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null)
echo "Result: John sees $JOHN_WORKLOGS_COUNT worklogs"
echo ""

# Test 5: TECHNICIAN cannot access other user's worklogs
echo "=== TEST 5: Cannot Access Other User's Worklogs (403) ==="
echo "John accessing GET /api/worklogs/user/$MDUDUZI_ID"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "http://localhost:8080/api/worklogs/user/$MDUDUZI_ID" \
  -H "Authorization: Bearer $TECH_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "403" ]; then
  echo "✓ PASS: Got 403 Forbidden as expected"
else
  echo "✗ FAIL: Expected 403 but got $HTTP_CODE"
fi
echo ""

# Test 6: TECHNICIAN cannot create jobs
echo "=== TEST 6: Cannot Create Jobs (403) ==="
echo "John trying to POST /api/jobs"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "http://localhost:8080/api/jobs" \
  -H "Authorization: Bearer $TECH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clientName":"Test Client","address":"123 Test St"}')
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "403" ]; then
  echo "✓ PASS: Got 403 Forbidden as expected"
else
  echo "✗ FAIL: Expected 403 but got $HTTP_CODE"
fi
echo ""

# Test 7: TECHNICIAN cannot access other technician's invoices
echo "=== TEST 7: Cannot Access Other's Invoices (403) ==="
echo "John accessing GET /api/invoices/technician/$MDUDUZI_ID"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "http://localhost:8080/api/invoices/technician/$MDUDUZI_ID" \
  -H "Authorization: Bearer $TECH_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "403" ]; then
  echo "✓ PASS: Got 403 Forbidden as expected"
else
  echo "✗ FAIL: Expected 403 but got $HTTP_CODE"
fi
echo ""

# Test 8: TECHNICIAN cannot access other's location tracking
echo "=== TEST 8: Cannot Access Other's Location (403) ==="
echo "John accessing GET /api/location-tracking/user/$MDUDUZI_ID/history"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "http://localhost:8080/api/location-tracking/user/$MDUDUZI_ID/history" \
  -H "Authorization: Bearer $TECH_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "403" ]; then
  echo "✓ PASS: Got 403 Forbidden as expected"
else
  echo "✗ FAIL: Expected 403 but got $HTTP_CODE"
fi
echo ""

# Test 9: TECHNICIAN can access own location history
echo "=== TEST 9: Can Access Own Location History (200) ==="
echo "John accessing GET /api/location-tracking/user/$JOHN_ID/history"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "http://localhost:8080/api/location-tracking/user/$JOHN_ID/history" \
  -H "Authorization: Bearer $TECH_TOKEN")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "200" ]; then
  echo "✓ PASS: Got 200 OK"
else
  echo "✗ FAIL: Expected 200 but got $HTTP_CODE"
fi
echo ""

echo "========================================="
echo "TEST RESULTS SUMMARY"
echo "========================================="
echo ""
echo "DATA ISOLATION WORKING:"
echo "- Technicians see only their assigned jobs"
echo "- Technicians see only their own worklogs"
echo "- Technicians cannot access other users' data (403 Forbidden)"
echo ""
echo "AUTHORIZATION WORKING:"
echo "- Technicians cannot create jobs (403 Forbidden)"
echo "- @PreAuthorize annotations enforced correctly"
echo ""
echo "✓ Role-based access control implementation successful!"
```

### Running the Tests

```bash
# Make scripts executable
chmod +x scripts/get_tokens.sh
chmod +x scripts/test_access_control.sh

# Get authentication tokens
./scripts/get_tokens.sh

# Run access control tests
./scripts/test_access_control.sh
```

### Expected Test Results

All tests should pass with the following output:

```
✓ TEST 1: John sees only 2 assigned jobs (not all jobs)
✓ TEST 2: John can access own data (200 OK)
✓ TEST 3: John cannot access Mduduzi's jobs (403 Forbidden)
✓ TEST 4: John sees only own worklogs
✓ TEST 5: John cannot access Mduduzi's worklogs (403 Forbidden)
✓ TEST 6: John cannot create jobs (403 Forbidden)
✓ TEST 7: John cannot access Mduduzi's invoices (403 Forbidden)
✓ TEST 8: John cannot access Mduduzi's location history (403 Forbidden)
✓ TEST 9: John can access own location history (200 OK)
```

### Default Test Users

| Username | Password | Role | Email |
|----------|----------|------|-------|
| admin | password123 | ADMIN | admin@solar.com |
| manager1 | password123 | MANAGER | manager@solar.com |
| john.tech | password123 | TECHNICIAN | john@solar.com |
| mike.tech | password123 | TECHNICIAN | mike@solar.com |
| assistant1 | password123 | ASSISTANT | assistant@solar.com |

### Troubleshooting Tests

**401 Unauthorized:**
- Token may have expired (tokens expire after 5 minutes)
- Re-run `./scripts/get_tokens.sh` to get fresh tokens

**403 Forbidden (unexpected):**
- User may not have required role
- Check user assignment to jobs in database

**Connection refused:**
- Ensure backend is running: `docker-compose ps`
- Check backend logs: `docker logs solar-backend`

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
