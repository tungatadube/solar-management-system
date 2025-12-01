# Complete System Deployment Guide

## What's Been Updated

✅ **Frontend Components Completed:**
- Stock Management - Full CRUD with low stock alerts
- Location Tracking - Real-time GPS with map visualization
- Reports & Invoicing - Generate and download invoices

✅ **Backend Integration:**
- All API endpoints connected
- Docker networking configured
- Nginx reverse proxy setup

✅ **Invoice System:**
- Work log tracking
- Automated invoice generation
- Excel export matching your format

---

## Quick Deploy (Docker)

### 1. Copy Files to Your System

```bash
# Extract the updated ZIP
cd your-working-directory
unzip solar-management-system-with-invoicing.zip

# Navigate to project
cd solar-management-system
```

### 2. Rebuild and Restart

```bash
# Stop current containers
docker-compose down

# Rebuild with new code
docker-compose build

# Start everything
docker-compose up -d

# Watch logs
docker-compose logs -f
```

### 3. Access the System

- **Frontend:** http://localhost:3000
- **Backend API:** http://localhost:8080
- **Database:** localhost:5432

---

## What's New in This Update

### Frontend Components

**1. Stock Management** (`/stock`)
- Add/edit/delete stock items
- Low stock alerts
- Category-based organization
- Barcode support
- Multi-location tracking ready

**2. Location Tracking** (`/tracking`)
- Real-time GPS tracking
- Location history with map
- Path visualization
- Speed and accuracy display
- Works with browser geolocation

**3. Reports & Invoicing** (`/reports`)
- Generate invoices for date ranges
- Download Excel invoices
- View all invoices
- Track uninvoiced work
- Financial summaries

### Backend Updates

**New Entities:**
- `WorkLog` - Track hours worked
- `Invoice` - Complete invoice records

**New Controllers:**
- `InvoiceController` - `/api/invoices`
- `WorkLogController` - `/api/worklogs`

**New Services:**
- `InvoiceService` - Excel generation

### Docker Configuration

**Updated nginx.conf:**
- Proxies `/api` requests to backend
- Serves React app on port 80
- Proper headers for proxy

**Updated docker-compose.yml:**
- Backend accessible as `backend:8080`
- Frontend on port 3000 → 80
- Proper network configuration

---

## API Endpoints Now Available

### Stock Management
```
GET    /api/stock
POST   /api/stock
GET    /api/stock/{id}
PUT    /api/stock/{id}
DELETE /api/stock/{id}
GET    /api/stock/low-stock
```

### Work Logs
```
GET    /api/worklogs
POST   /api/worklogs
GET    /api/worklogs/user/{id}
GET    /api/worklogs/user/{id}/uninvoiced
PUT    /api/worklogs/{id}
DELETE /api/worklogs/{id}
```

### Invoices
```
POST   /api/invoices/generate
POST   /api/invoices/{id}/generate-excel
GET    /api/invoices/{id}/download
GET    /api/invoices/technician/{id}
PUT    /api/invoices/{id}
```

### Jobs, Locations, Users
All existing endpoints remain available.

---

## Testing the System

### 1. Test Stock Management

```bash
# Add a stock item
curl -X POST http://localhost:8080/api/stock \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "BAT-001",
    "name": "Tesla Powerwall 2",
    "category": "BATTERY",
    "unit": "piece",
    "unitPrice": 12000,
    "minimumQuantity": 2,
    "reorderLevel": 5
  }'

# Get all stock
curl http://localhost:8080/api/stock
```

### 2. Test Work Logs & Invoices

```bash
# Create a work log
curl -X POST http://localhost:8080/api/worklogs \
  -H "Content-Type: application/json" \
  -d '{
    "user": {"id": 1},
    "job": {"id": 1},
    "workDate": "2025-11-29",
    "startTime": "08:30:00",
    "endTime": "12:00:00",
    "hourlyRate": 35.00,
    "workDescription": "Battery and Inverter Installation",
    "jobAddress": "123 Solar Street",
    "workType": "BATTERY_INSTALLATION"
  }'

# Generate invoice
curl -X POST "http://localhost:8080/api/invoices/generate?technicianId=1&startDate=2025-11-25&endDate=2025-11-29"

# Download invoice
curl -O http://localhost:8080/api/invoices/1/download
```

### 3. Test Location Tracking

Open http://localhost:3000/tracking in browser and click "Start Tracking"

---

## Troubleshooting

### Frontend can't connect to backend

**Check nginx proxy:**
```bash
docker exec solar-frontend cat /etc/nginx/conf.d/default.conf
```

**Should show:**
```nginx
location /api {
    proxy_pass http://backend:8080;
    ...
}
```

**Check backend is accessible:**
```bash
docker exec solar-frontend curl http://backend:8080/api/jobs
```

### Database connection issues

**Check PostgreSQL is running:**
```bash
docker ps | grep postgres
```

**Check backend logs:**
```bash
docker logs solar-backend
```

### Port already in use

**If port 3000 is busy:**
```yaml
# In docker-compose.yml, change:
ports:
  - "8000:80"  # Use port 8000 instead
```

---

## Environment Variables

### Backend
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/solar_management
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
JWT_SECRET: your-secret-key
```

### Frontend
```yaml
REACT_APP_API_URL: /api  # Relative URL for nginx proxy
```

---

## Database Migrations

When you restart with new code, JPA will automatically:
- Create `work_logs` table
- Create `invoices` table
- Add necessary foreign keys

**Check tables created:**
```bash
docker exec solar-postgres psql -U postgres -d solar_management -c "\dt"
```

---

## Next Steps

### 1. Create Test Data

Visit http://localhost:3000 and:
1. Add some stock items in Stock Management
2. Create a user (technician)
3. Create jobs
4. Add work logs for those jobs
5. Generate an invoice

### 2. Customize Invoice Format

Edit `InvoiceService.java` to match your specific needs:
- Company logo
- Additional fields
- Custom calculations

### 3. Add Security

Currently, there's no authentication. To add:
1. Implement JWT login
2. Protect API endpoints
3. Add user roles

### 4. Mobile App

The Location Tracking works in mobile browsers. For native app:
- Use React Native
- Call the same APIs
- Add background location tracking

---

## File Structure

```
solar-management-system/
├── backend/
│   ├── src/main/java/.../
│   │   ├── entity/
│   │   │   ├── WorkLog.java          ← NEW
│   │   │   └── Invoice.java          ← NEW
│   │   ├── repository/
│   │   │   ├── WorkLogRepository.java ← NEW
│   │   │   └── InvoiceRepository.java ← NEW
│   │   ├── service/
│   │   │   └── InvoiceService.java    ← NEW
│   │   └── controller/
│   │       ├── InvoiceController.java ← NEW
│   │       └── WorkLogController.java ← NEW
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── pages/
│   │   │   ├── StockManagement.tsx    ← UPDATED
│   │   │   ├── LocationTracking.tsx   ← UPDATED
│   │   │   └── Reports.tsx            ← UPDATED
│   │   ├── services/
│   │   │   └── api.ts                 ← UPDATED
│   │   └── types/
│   │       └── index.ts               ← UPDATED
│   ├── nginx.conf                     ← UPDATED
│   └── Dockerfile
└── docker-compose.yml                 ← UPDATED
```

---

## Success Criteria

✅ Frontend loads at http://localhost:3000  
✅ All navigation tabs work (Dashboard, Jobs, Stock, Tracking, Reports)  
✅ Can add stock items  
✅ Can track location on map  
✅ Can generate and download invoices  
✅ API calls work (check Network tab in browser DevTools)  

---

## Support

If you encounter issues:

1. **Check logs:**
   ```bash
   docker-compose logs backend
   docker-compose logs frontend
   ```

2. **Verify network:**
   ```bash
   docker network inspect solar-management-system_solar-network
   ```

3. **Test API directly:**
   ```bash
   curl http://localhost:8080/api/jobs
   ```

4. **Restart everything:**
   ```bash
   docker-compose down
   docker-compose up --build
   ```

---

**Your complete Solar Management System is ready!** 🎉

All components are now fully functional and connected.
