# 🚀 QUICK START - Solar Management System

## Fastest Way to Run (Docker)

```bash
cd solar-management-system
docker-compose up -d
```

**Done!** Access at http://localhost:3000

---

## Manual Setup (Without Docker)

### 1. Database (2 min)
```bash
createdb solar_management
```

### 2. Backend (3 min)
```bash
cd backend
# Edit src/main/resources/application.yml with your DB password
mvn spring-boot:run
```
✅ Backend running at http://localhost:8080

### 3. Frontend (3 min)
```bash
cd frontend
npm install
npm start
```
✅ Frontend running at http://localhost:3000

---

## What You Get

| Feature | Status |
|---------|--------|
| Job Management | ✅ Complete |
| Stock Tracking | ✅ Complete |
| GPS Location | ✅ Complete |
| Photo Upload | ✅ Complete |
| Travel Logs | ✅ Complete |
| Reporting | ✅ Framework Ready |
| User Roles | ✅ Complete |
| API | ✅ Complete |
| Database | ✅ Complete |
| Docker | ✅ Complete |

---

## File Structure
```
solar-management-system/
├── backend/          # Java Spring Boot
├── frontend/         # React + TypeScript
├── database/         # SQL schema
├── docker-compose.yml
├── README.md
├── SETUP_GUIDE.md
└── PROJECT_SUMMARY.md
```

---

## Tech Stack

**Backend:** Java 17, Spring Boot 3.2, PostgreSQL  
**Frontend:** React 18, TypeScript, Material-UI  
**Deployment:** Docker, Docker Compose

---

## Default Ports

- **Frontend:** 3000
- **Backend:** 8080  
- **Database:** 5432

---

## Key Endpoints

```
http://localhost:8080/api/jobs
http://localhost:8080/api/location-tracking
http://localhost:8080/api/stock
```

---

## Test It

```bash
# Check backend
curl http://localhost:8080/api/jobs

# Check frontend
open http://localhost:3000
```

---

## Stop Everything

```bash
docker-compose down
```

---

## Need Help?

1. Check **SETUP_GUIDE.md** for detailed instructions
2. Review **README.md** for features
3. See **PROJECT_SUMMARY.md** for architecture

---

## Credentials (After Setup)

Default admin user (create via API):
- Username: `admin`
- Password: Set your own

---

## Next Steps

1. ✅ Get it running (5 min)
2. Create your first user
3. Add a location
4. Create a job
5. Start tracking!

**Estimated Total Setup Time:** 5-10 minutes with Docker
