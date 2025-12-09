# AWS Deployment Guide - Solar Management System

## Overview

This guide covers deploying your full-stack application to AWS using:
- **S3 + CloudFront**: Frontend (React)
- **EC2 or ECS**: Backend (Spring Boot) + Keycloak
- **RDS**: PostgreSQL Database

## Prerequisites

1. AWS Account
2. AWS CLI installed and configured
3. Domain name (optional but recommended)

## Part 1: Frontend Deployment (S3 + CloudFront)

### Step 1: Configure Environment Variables

Update `frontend/.env.production`:
```env
REACT_APP_API_URL=https://api.yourdomain.com/api
```

### Step 2: Deploy to S3

```bash
cd frontend
./deploy-s3.sh
```

Or manually:

```bash
# Build
npm run build

# Create S3 bucket
aws s3 mb s3://solar-management-frontend --region ap-southeast-2

# Upload files
aws s3 sync build/ s3://solar-management-frontend/ --delete

# Enable static website hosting
aws s3 website s3://solar-management-frontend/ \
  --index-document index.html \
  --error-document index.html
```

### Step 3: Create CloudFront Distribution

1. Go to AWS Console → CloudFront → Create Distribution
2. **Origin Domain**: Select your S3 bucket
3. **Viewer Protocol Policy**: Redirect HTTP to HTTPS
4. **Allowed HTTP Methods**: GET, HEAD, OPTIONS
5. **Compress Objects Automatically**: Yes
6. **Price Class**: Use only North America and Europe (or your preferred regions)
7. **Alternate Domain Names (CNAMEs)**: yourdomain.com, www.yourdomain.com
8. **SSL Certificate**: Request or import a certificate from ACM
9. **Default Root Object**: index.html
10. **Custom Error Responses**:
    - Error Code: 403 → Response: /index.html (200)
    - Error Code: 404 → Response: /index.html (200)

### Step 4: Configure Route 53 (if using custom domain)

1. Go to Route 53 → Hosted Zones
2. Create A record pointing to CloudFront distribution
3. Create AAAA record (IPv6) pointing to CloudFront distribution

---

## Part 2: Database Deployment (RDS)

### Step 1: Create RDS PostgreSQL Instance

```bash
# Using AWS CLI
aws rds create-db-instance \
  --db-instance-identifier solar-management-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.3 \
  --master-username postgres \
  --master-user-password YOUR_SECURE_PASSWORD \
  --allocated-storage 20 \
  --vpc-security-group-ids sg-xxxxx \
  --db-subnet-group-name default \
  --backup-retention-period 7 \
  --publicly-accessible false
```

Or using AWS Console:
1. Go to RDS → Create Database
2. Choose PostgreSQL
3. Select Free Tier (or appropriate size)
4. Set master username and password
5. Configure VPC and security groups
6. Enable automated backups

### Step 2: Initialize Database

```bash
# Connect to RDS instance
psql -h your-rds-endpoint.rds.amazonaws.com \
  -U postgres \
  -d postgres \
  -f database/schema.sql
```

---

## Part 3: Backend Deployment (EC2)

### Option A: Deploy to EC2 (Simple)

#### Step 1: Launch EC2 Instance

1. Go to EC2 → Launch Instance
2. Choose Amazon Linux 2023 or Ubuntu
3. Instance type: t3.small or larger
4. Configure security group:
   - Port 22 (SSH)
   - Port 8080 (Backend)
   - Port 8180 (Keycloak)
5. Create or select key pair

#### Step 2: Connect and Setup

```bash
# SSH into instance
ssh -i your-key.pem ec2-user@your-instance-public-ip

# Install Java
sudo yum install -y java-17-amazon-corretto

# Install Docker
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# Copy your code
scp -i your-key.pem -r backend ec2-user@your-instance-public-ip:~
scp -i your-key.pem docker-compose.yml ec2-user@your-instance-public-ip:~
```

#### Step 3: Configure Environment

Create `.env` file on EC2:
```env
DB_HOST=your-rds-endpoint.rds.amazonaws.com
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=YOUR_SECURE_PASSWORD

KEYCLOAK_URL=http://localhost:8180
KEYCLOAK_REALM=solar-management
KEYCLOAK_CLIENT_ID=solar-backend
```

#### Step 4: Run with Docker Compose

```bash
# Update docker-compose.yml to use RDS
# Remove postgres service
# Update backend environment variables

docker-compose up -d
```

### Option B: Deploy to ECS (Production-Ready)

#### Step 1: Build and Push Docker Images

```bash
# Login to ECR
aws ecr get-login-password --region ap-southeast-2 | \
  docker login --username AWS --password-stdin \
  YOUR_ACCOUNT_ID.dkr.ecr.ap-southeast-2.amazonaws.com

# Create repositories
aws ecr create-repository --repository-name solar-backend
aws ecr create-repository --repository-name solar-keycloak

# Build and push backend
cd backend
docker build -t solar-backend .
docker tag solar-backend:latest \
  YOUR_ACCOUNT_ID.dkr.ecr.ap-southeast-2.amazonaws.com/solar-backend:latest
docker push YOUR_ACCOUNT_ID.dkr.ecr.ap-southeast-2.amazonaws.com/solar-backend:latest
```

#### Step 2: Create ECS Cluster

```bash
aws ecs create-cluster --cluster-name solar-management-cluster
```

#### Step 3: Create Task Definition

Create `ecs-task-definition.json`:
```json
{
  "family": "solar-management",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "containerDefinitions": [
    {
      "name": "backend",
      "image": "YOUR_ACCOUNT_ID.dkr.ecr.ap-southeast-2.amazonaws.com/solar-backend:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {"name": "DB_HOST", "value": "your-rds-endpoint.rds.amazonaws.com"},
        {"name": "DB_NAME", "value": "postgres"},
        {"name": "DB_USER", "value": "postgres"}
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:region:account:secret:db-password"
        }
      ]
    }
  ]
}
```

#### Step 4: Create Service

```bash
aws ecs create-service \
  --cluster solar-management-cluster \
  --service-name backend-service \
  --task-definition solar-management \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
```

---

## Part 4: Load Balancer & SSL

### Step 1: Create Application Load Balancer

1. Go to EC2 → Load Balancers → Create Load Balancer
2. Choose Application Load Balancer
3. Configure:
   - Scheme: Internet-facing
   - IP address type: IPv4
   - Listeners: HTTP (80) and HTTPS (443)
   - Availability Zones: Select at least 2

### Step 2: Configure Target Groups

1. Create target group for backend (port 8080)
2. Register EC2 instances or ECS tasks
3. Configure health checks:
   - Path: /actuator/health
   - Interval: 30 seconds

### Step 3: Add SSL Certificate

1. Go to Certificate Manager (ACM)
2. Request certificate for api.yourdomain.com
3. Validate via DNS or Email
4. Add certificate to load balancer HTTPS listener

---

## Part 5: Environment Variables & Secrets

### Using AWS Secrets Manager

```bash
# Store database password
aws secretsmanager create-secret \
  --name solar-management/db-password \
  --secret-string "YOUR_SECURE_PASSWORD"

# Store Keycloak secret
aws secretsmanager create-secret \
  --name solar-management/keycloak-secret \
  --secret-string "YOUR_KEYCLOAK_SECRET"
```

---

## Deployment Checklist

- [ ] Frontend built and deployed to S3
- [ ] CloudFront distribution created with SSL
- [ ] Custom domain configured in Route 53
- [ ] RDS PostgreSQL instance created
- [ ] Database schema initialized
- [ ] Backend deployed to EC2/ECS
- [ ] Keycloak deployed and configured
- [ ] Load balancer configured with SSL
- [ ] Environment variables and secrets configured
- [ ] Security groups configured (restrict access)
- [ ] Backups enabled (RDS automatic backups)
- [ ] CloudWatch logs and monitoring set up
- [ ] Cost alerts configured

---

## Cost Estimation (Monthly)

- **S3 + CloudFront**: ~$5-20 (depending on traffic)
- **RDS (db.t3.micro)**: ~$15-25
- **EC2 (t3.small)**: ~$15-20
- **ECS Fargate**: ~$30-50 (if using ECS)
- **Load Balancer**: ~$20-30
- **Total**: ~$85-145/month

---

## Monitoring & Maintenance

### CloudWatch Alarms

```bash
# CPU utilization
aws cloudwatch put-metric-alarm \
  --alarm-name high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold
```

### Backup Strategy

1. **RDS**: Automated backups (7-day retention)
2. **S3**: Versioning enabled
3. **Code**: GitHub repository

---

## Troubleshooting

### Frontend not loading
- Check S3 bucket policy
- Verify CloudFront distribution status
- Check CORS settings

### Backend connection issues
- Verify security group rules
- Check RDS connectivity
- Review application logs in CloudWatch

### 502/504 errors
- Check target group health
- Verify backend is running
- Check health check configuration

---

## Additional Resources

- [AWS Well-Architected Framework](https://aws.amazon.com/architecture/well-architected/)
- [AWS Free Tier](https://aws.amazon.com/free/)
- [AWS CLI Documentation](https://docs.aws.amazon.com/cli/)
