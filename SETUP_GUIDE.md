# Solar Management System - Setup & Deployment Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Docker Deployment](#docker-deployment)
4. [Production Deployment](#production-deployment)
5. [Configuration](#configuration)
6. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software
- **Java Development Kit (JDK) 17 or higher**
  ```bash
  # Check Java version
  java -version
  ```

- **Node.js 18+ and npm**
  ```bash
  # Check Node version
  node --version
  npm --version
  ```

- **PostgreSQL 15+**
  ```bash
  # Check PostgreSQL version
  psql --version
  ```

- **Maven 3.8+** (or use Maven wrapper included in project)
  ```bash
  # Check Maven version
  mvn --version
  ```

### Optional Tools
- Docker Desktop (for containerized deployment)
- Git (for version control)
- Postman or similar (for API testing)

## Local Development Setup

### Step 1: Database Setup

1. **Install PostgreSQL** (if not already installed)
   - Windows: Download from https://www.postgresql.org/download/windows/
   - macOS: `brew install postgresql@15`
   - Linux: `sudo apt-get install postgresql-15`

2. **Create Database**
   ```bash
   # Start PostgreSQL service
   # macOS
   brew services start postgresql@15
   
   # Linux
   sudo systemctl start postgresql
   
   # Create database
   createdb solar_management
   
   # Or using psql
   psql -U postgres
   CREATE DATABASE solar_management;
   \q
   ```

3. **Verify Database**
   ```bash
   psql -U postgres -d solar_management -c "SELECT version();"
   ```

### Step 2: Backend Setup

1. **Navigate to backend directory**
   ```bash
   cd solar-management-system/backend
   ```

2. **Configure Database Connection**
   Edit `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/solar_management
       username: postgres  # Your PostgreSQL username
       password: postgres  # Your PostgreSQL password
   ```

3. **Build the Application**
   ```bash
   # Using Maven
   mvn clean install
   
   # Or using Maven wrapper
   ./mvnw clean install
   ```

4. **Run the Backend**
   ```bash
   mvn spring-boot:run
   
   # Or
   ./mvnw spring-boot:run
   ```

5. **Verify Backend is Running**
   - Open browser to: http://localhost:8080
   - You should see a 404 page (this is normal, the API is running)
   - Test API: http://localhost:8080/api/jobs

### Step 3: Frontend Setup

1. **Navigate to frontend directory**
   ```bash
   cd solar-management-system/frontend
   ```

2. **Install Dependencies**
   ```bash
   npm install
   ```

3. **Configure API URL**
   Create `.env` file in frontend directory:
   ```
   REACT_APP_API_URL=http://localhost:8080/api
   ```

4. **Start Development Server**
   ```bash
   npm start
   ```

5. **Access the Application**
   - Open browser to: http://localhost:3000
   - You should see the Solar Management dashboard

### Step 4: Create Initial User

You can create an initial user through the API:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@solar.com",
    "password": "admin123",
    "firstName": "Admin",
    "lastName": "User",
    "role": "ADMIN"
  }'
```

## Docker Deployment

### Quick Start with Docker Compose

1. **Ensure Docker is Running**
   ```bash
   docker --version
   docker-compose --version
   ```

2. **Start All Services**
   ```bash
   cd solar-management-system
   docker-compose up -d
   ```

3. **Check Container Status**
   ```bash
   docker-compose ps
   ```

4. **View Logs**
   ```bash
   # All services
   docker-compose logs -f
   
   # Specific service
   docker-compose logs -f backend
   docker-compose logs -f frontend
   docker-compose logs -f postgres
   ```

5. **Stop Services**
   ```bash
   docker-compose down
   
   # Remove volumes as well
   docker-compose down -v
   ```

### Individual Container Management

**Backend Only**
```bash
cd backend
docker build -t solar-backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/solar_management \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  solar-backend
```

**Frontend Only**
```bash
cd frontend
docker build -t solar-frontend .
docker run -p 3000:3000 solar-frontend
```

## Production Deployment

### Backend (Spring Boot)

1. **Build Production JAR**
   ```bash
   cd backend
   mvn clean package -DskipTests -Pprod
   ```

2. **Deploy JAR**
   ```bash
   # Copy JAR to server
   scp target/management-system-1.0.0.jar user@server:/opt/solar-management/
   
   # SSH to server and run
   ssh user@server
   cd /opt/solar-management
   java -jar management-system-1.0.0.jar \
     --spring.profiles.active=prod \
     --spring.datasource.url=jdbc:postgresql://prod-db:5432/solar_management \
     --spring.datasource.username=prod_user \
     --spring.datasource.password=secure_password \
     --jwt.secret=your-production-secret-key
   ```

3. **Create Systemd Service (Linux)**
   Create `/etc/systemd/system/solar-backend.service`:
   ```ini
   [Unit]
   Description=Solar Management Backend
   After=postgresql.service
   
   [Service]
   User=solarapp
   WorkingDirectory=/opt/solar-management
   ExecStart=/usr/bin/java -jar management-system-1.0.0.jar --spring.profiles.active=prod
   Restart=always
   RestartSec=10
   StandardOutput=syslog
   StandardError=syslog
   SyslogIdentifier=solar-backend
   
   [Install]
   WantedBy=multi-user.target
   ```
   
   Enable and start:
   ```bash
   sudo systemctl enable solar-backend
   sudo systemctl start solar-backend
   sudo systemctl status solar-backend
   ```

### Frontend (React)

1. **Build Production Bundle**
   ```bash
   cd frontend
   npm run build
   ```

2. **Deploy with Nginx**
   
   Install Nginx:
   ```bash
   sudo apt-get install nginx
   ```
   
   Create Nginx configuration `/etc/nginx/sites-available/solar-management`:
   ```nginx
   server {
       listen 80;
       server_name solar.yourdomain.com;
       root /var/www/solar-management;
       index index.html;
   
       location / {
           try_files $uri $uri/ /index.html;
       }
   
       location /api {
           proxy_pass http://localhost:8080;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection 'upgrade';
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
           proxy_cache_bypass $http_upgrade;
       }
   }
   ```
   
   Deploy files:
   ```bash
   # Copy build files
   sudo cp -r build/* /var/www/solar-management/
   
   # Enable site
   sudo ln -s /etc/nginx/sites-available/solar-management /etc/nginx/sites-enabled/
   
   # Test configuration
   sudo nginx -t
   
   # Reload Nginx
   sudo systemctl reload nginx
   ```

3. **Enable HTTPS with Let's Encrypt**
   ```bash
   sudo apt-get install certbot python3-certbot-nginx
   sudo certbot --nginx -d solar.yourdomain.com
   ```

### Database (PostgreSQL)

1. **Production Configuration**
   ```bash
   # Edit postgresql.conf
   max_connections = 100
   shared_buffers = 256MB
   effective_cache_size = 1GB
   maintenance_work_mem = 64MB
   checkpoint_completion_target = 0.9
   wal_buffers = 16MB
   default_statistics_target = 100
   random_page_cost = 1.1
   effective_io_concurrency = 200
   work_mem = 2621kB
   min_wal_size = 1GB
   max_wal_size = 4GB
   ```

2. **Backup Strategy**
   ```bash
   # Daily backup script
   #!/bin/bash
   BACKUP_DIR="/backups/solar-management"
   DATE=$(date +%Y%m%d_%H%M%S)
   
   pg_dump solar_management | gzip > $BACKUP_DIR/solar_$DATE.sql.gz
   
   # Keep only last 30 days
   find $BACKUP_DIR -name "solar_*.sql.gz" -mtime +30 -delete
   ```
   
   Add to crontab:
   ```bash
   0 2 * * * /usr/local/bin/backup-solar-db.sh
   ```

## Configuration

### Environment Variables

Create `.env` file or set system environment variables:

**Backend**
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=solar_management
DB_USERNAME=postgres
DB_PASSWORD=your_password

# Security
JWT_SECRET=your-very-long-secret-key-minimum-256-bits
JWT_EXPIRATION=86400000

# AWS S3 (optional)
AWS_ACCESS_KEY=your_access_key
AWS_SECRET_KEY=your_secret_key
AWS_S3_BUCKET=solar-images
AWS_REGION=ap-southeast-2

# File Upload
FILE_UPLOAD_DIR=./uploads
MAX_FILE_SIZE=10MB
```

**Frontend**
```bash
REACT_APP_API_URL=http://localhost:8080/api
REACT_APP_MAP_CENTER_LAT=-34.9285
REACT_APP_MAP_CENTER_LNG=138.6007
```

## Troubleshooting

### Common Issues

1. **Backend won't start - "Connection refused"**
   ```bash
   # Check if PostgreSQL is running
   sudo systemctl status postgresql
   
   # Check if database exists
   psql -U postgres -l | grep solar_management
   
   # Check connection
   psql -U postgres -d solar_management -c "SELECT 1;"
   ```

2. **Port 8080 already in use**
   ```bash
   # Find process using port
   lsof -i :8080
   
   # Kill process
   kill -9 <PID>
   
   # Or change port in application.yml
   server:
     port: 8081
   ```

3. **Frontend can't connect to backend (CORS error)**
   ```yaml
   # Update application.yml
   app:
     cors:
       allowed-origins: http://localhost:3000,http://localhost:4200
   ```

4. **Out of memory error**
   ```bash
   # Increase JVM heap size
   java -Xmx2g -jar management-system-1.0.0.jar
   ```

5. **Database connection pool exhausted**
   ```yaml
   # Adjust in application.yml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
   ```

### Logs

**Backend Logs**
```bash
# Development
tail -f logs/spring-boot-logger.log

# Production with systemd
sudo journalctl -u solar-backend -f
```

**Frontend Logs**
```bash
# Development server
# Logs appear in terminal

# Production nginx
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/access.log
```

**Database Logs**
```bash
# PostgreSQL logs
sudo tail -f /var/log/postgresql/postgresql-15-main.log
```

### Performance Monitoring

1. **Enable Spring Boot Actuator** (already included)
   ```yaml
   # application.yml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics
   ```
   
   Access: http://localhost:8080/actuator/health

2. **Database Performance**
   ```sql
   -- Check slow queries
   SELECT * FROM pg_stat_statements 
   ORDER BY mean_exec_time DESC 
   LIMIT 10;
   
   -- Check connection usage
   SELECT * FROM pg_stat_activity;
   ```

## Security Checklist

- [ ] Change default JWT secret
- [ ] Use strong database passwords
- [ ] Enable HTTPS in production
- [ ] Configure firewall rules
- [ ] Regular security updates
- [ ] Implement rate limiting
- [ ] Enable SQL injection protection
- [ ] Secure file upload directory
- [ ] Regular database backups
- [ ] Monitor application logs
- [ ] Use environment variables for secrets
- [ ] Implement proper CORS policies

## Support

For additional help:
- Check the main README.md
- Review API documentation
- Contact: support@solarmanagement.com
