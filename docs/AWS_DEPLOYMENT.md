# AWS Deployment Guide: Solar Management System

This guide provides step-by-step instructions for deploying the Solar Management System on AWS using the **free tier**.

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Cost Breakdown](#cost-breakdown)
4. [Deployment Steps](#deployment-steps)
5. [Post-Deployment Configuration](#post-deployment-configuration)
6. [Accessing the Application](#accessing-the-application)
7. [Monitoring and Maintenance](#monitoring-and-maintenance)
8. [Troubleshooting](#troubleshooting)
9. [Scaling Options](#scaling-options)

---

## Architecture Overview

### Infrastructure Components

- **EC2 Instance**: t2.micro (1 vCPU, 1GB RAM) - FREE tier
  - Hosts Docker containers for both production and staging environments
  - Production ports: 80 (Frontend), 8080 (Backend), 8180 (Keycloak)
  - Staging ports: 3001 (Frontend), 8081 (Backend), 8181 (Keycloak)

- **RDS PostgreSQL**: db.t3.micro (1 vCPU, 1GB RAM, 20GB storage) - FREE tier
  - Databases: `keycloak_prod`, `solar_management_prod`, `keycloak_staging`, `solar_management_staging`
  - Automated backups: 7-day retention (FREE)

- **S3 Bucket**: `solar-management-prod-uploads` - 5GB FREE
  - Folders: `/production/` and `/staging/`

- **Elastic IP**: 1 free when attached to running instance

### Network Architecture

```
Internet → Elastic IP → EC2 (Docker Containers) → RDS PostgreSQL
                     ↓
                  S3 Bucket
```

---

## Prerequisites

Before starting, ensure you have:

1. **AWS Account** with free tier eligibility
2. **AWS CLI** installed and configured
   ```bash
   aws configure
   ```
3. **SSH Client** (ssh command available)
4. **Git** for repository management
5. **Google Maps API Key** (optional, for map features)

---

## Cost Breakdown

### Within Free Tier (First 12 Months)
- EC2 t2.micro: **$0/month** (750 hours)
- RDS db.t3.micro: **$0/month** (750 hours)
- EBS 20GB (RDS): **$0/month**
- EBS 8GB (EC2): **$0/month**
- S3 5GB storage: **$0/month**
- Elastic IP: **$0/month** (when attached)
- Data Transfer 15GB: **$0/month**

**Total: $0/month** ✅

### After Free Tier Expires (Month 13+)
- EC2 t2.micro: ~$8.40/month
- RDS db.t3.micro: ~$13.50/month
- EBS 30GB: ~$3.00/month
- S3 + transfers: ~$2-5/month

**Total: ~$27-30/month**

---

## Deployment Steps

### Phase 1: AWS Infrastructure Setup

#### Step 1: Create RDS PostgreSQL Database

**Via AWS Console:**
1. Go to RDS Dashboard → Create database
2. Choose PostgreSQL (version 15.x)
3. Templates: Free tier
4. Settings:
   - DB instance identifier: `solar-rds`
   - Master username: `postgres`
   - Master password: (choose a strong password)
5. Instance configuration: db.t3.micro
6. Storage: 20 GB gp3
7. Connectivity:
   - Public access: No
   - VPC security group: Create new `solar-rds-sg`
8. Additional configuration:
   - Initial database name: (leave empty, create manually)
   - Backup retention: 7 days
   - Enable automated backups
9. Click "Create database"

**Via AWS CLI:**
```bash
aws rds create-db-instance \
  --db-instance-identifier solar-rds \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username postgres \
  --master-user-password YOUR_SECURE_PASSWORD \
  --allocated-storage 20 \
  --storage-type gp3 \
  --no-publicly-accessible \
  --backup-retention-period 7 \
  --vpc-security-group-ids sg-xxxxxxxxx \
  --region ap-southeast-2
```

**Wait for RDS to be available (5-10 minutes)**:
```bash
aws rds wait db-instance-available --db-instance-identifier solar-rds
```

**Note the RDS endpoint** (you'll need this later):
```bash
aws rds describe-db-instances \
  --db-instance-identifier solar-rds \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

#### Step 2: Create Databases in RDS

Once RDS is available, create the databases:

1. **Temporarily allow access from your IP**:
   - Edit `solar-rds-sg` security group
   - Add inbound rule: PostgreSQL (5432) from your IP

2. **Connect and create databases**:
```bash
# Get RDS endpoint
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier solar-rds \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

# Connect to RDS
psql -h $RDS_ENDPOINT -U postgres -p 5432

# In psql prompt:
CREATE DATABASE keycloak_prod;
CREATE DATABASE solar_management_prod;
CREATE DATABASE keycloak_staging;
CREATE DATABASE solar_management_staging;
\l  -- List databases to confirm
\q  -- Exit
```

3. **Remove public access rule** from `solar-rds-sg`

#### Step 3: Create S3 Bucket

```bash
# Create bucket
aws s3 mb s3://solar-management-prod-uploads --region ap-southeast-2

# Block all public access
aws s3api put-public-access-block \
  --bucket solar-management-prod-uploads \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Enable server-side encryption
aws s3api put-bucket-encryption \
  --bucket solar-management-prod-uploads \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }]
  }'
```

#### Step 4: Create IAM Role for EC2

```bash
# Create trust policy document
cat > ec2-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"Service": "ec2.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }]
}
EOF

# Create IAM role
aws iam create-role \
  --role-name solar-management-ec2-role \
  --assume-role-policy-document file://ec2-trust-policy.json

# Create S3 access policy
cat > s3-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": [
      "s3:PutObject",
      "s3:GetObject",
      "s3:DeleteObject",
      "s3:ListBucket"
    ],
    "Resource": [
      "arn:aws:s3:::solar-management-prod-uploads",
      "arn:aws:s3:::solar-management-prod-uploads/*"
    ]
  }]
}
EOF

# Attach S3 policy to role
aws iam put-role-policy \
  --role-name solar-management-ec2-role \
  --policy-name S3AccessPolicy \
  --policy-document file://s3-policy.json

# Create instance profile
aws iam create-instance-profile \
  --instance-profile-name solar-management-ec2-profile

# Add role to instance profile
aws iam add-role-to-instance-profile \
  --instance-profile-name solar-management-ec2-profile \
  --role-name solar-management-ec2-role
```

#### Step 5: Create Security Groups

**EC2 Security Group:**
```bash
# Get default VPC ID
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=isDefault,Values=true" \
  --query 'Vpcs[0].VpcId' \
  --output text)

# Create EC2 security group
EC2_SG_ID=$(aws ec2 create-security-group \
  --group-name solar-ec2-sg \
  --description "Security group for Solar Management EC2" \
  --vpc-id $VPC_ID \
  --output text)

echo "EC2 Security Group ID: $EC2_SG_ID"

# Add inbound rules
# SSH (your IP only - replace 1.2.3.4 with your actual IP)
aws ec2 authorize-security-group-ingress \
  --group-id $EC2_SG_ID \
  --protocol tcp --port 22 \
  --cidr 1.2.3.4/32

# HTTP (production frontend)
aws ec2 authorize-security-group-ingress \
  --group-id $EC2_SG_ID \
  --protocol tcp --port 80 \
  --cidr 0.0.0.0/0

# Backend API (production)
aws ec2 authorize-security-group-ingress \
  --group-id $EC2_SG_ID \
  --protocol tcp --port 8080 \
  --cidr 0.0.0.0/0

# Keycloak (production)
aws ec2 authorize-security-group-ingress \
  --group-id $EC2_SG_ID \
  --protocol tcp --port 8180 \
  --cidr 0.0.0.0/0

# Staging ports (optional)
aws ec2 authorize-security-group-ingress \
  --group-id $EC2_SG_ID \
  --protocol tcp --port 3001 \
  --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
  --group-id $EC2_SG_ID \
  --protocol tcp --port 8081 \
  --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
  --group-id $EC2_SG_ID \
  --protocol tcp --port 8181 \
  --cidr 0.0.0.0/0
```

**RDS Security Group:**
```bash
# Get RDS security group ID
RDS_SG_ID=$(aws rds describe-db-instances \
  --db-instance-identifier solar-rds \
  --query 'DBInstances[0].VpcSecurityGroups[0].VpcSecurityGroupId' \
  --output text)

echo "RDS Security Group ID: $RDS_SG_ID"

# Allow PostgreSQL from EC2 security group
aws ec2 authorize-security-group-ingress \
  --group-id $RDS_SG_ID \
  --protocol tcp --port 5432 \
  --source-group $EC2_SG_ID
```

#### Step 6: Launch EC2 Instance

```bash
# Create SSH key pair
aws ec2 create-key-pair \
  --key-name solar-management-key \
  --query 'KeyMaterial' \
  --output text > solar-management-key.pem

chmod 400 solar-management-key.pem

# Get latest Amazon Linux 2023 AMI
AMI_ID=$(aws ec2 describe-images \
  --owners amazon \
  --filters "Name=name,Values=al2023-ami-*-x86_64" \
            "Name=state,Values=available" \
  --query 'Images | sort_by(@, &CreationDate) | [-1].ImageId' \
  --output text)

echo "Using AMI: $AMI_ID"

# Launch EC2 instance
INSTANCE_ID=$(aws ec2 run-instances \
  --image-id $AMI_ID \
  --instance-type t2.micro \
  --key-name solar-management-key \
  --security-group-ids $EC2_SG_ID \
  --iam-instance-profile Name=solar-management-ec2-profile \
  --user-data file://user-data.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=solar-management-prod}]' \
  --query 'Instances[0].InstanceId' \
  --output text)

echo "EC2 Instance ID: $INSTANCE_ID"

# Wait for instance to be running
aws ec2 wait instance-running --instance-ids $INSTANCE_ID
echo "Instance is running!"
```

#### Step 7: Allocate and Associate Elastic IP

```bash
# Allocate Elastic IP
ALLOCATION_ID=$(aws ec2 allocate-address \
  --domain vpc \
  --query 'AllocationId' \
  --output text)

# Get the Elastic IP address
ELASTIC_IP=$(aws ec2 describe-addresses \
  --allocation-ids $ALLOCATION_ID \
  --query 'Addresses[0].PublicIp' \
  --output text)

echo "Elastic IP: $ELASTIC_IP"

# Associate with EC2 instance
aws ec2 associate-address \
  --instance-id $INSTANCE_ID \
  --allocation-id $ALLOCATION_ID

echo "Elastic IP associated with instance"
echo ""
echo "====================================="
echo "IMPORTANT: Save these values!"
echo "====================================="
echo "Elastic IP: $ELASTIC_IP"
echo "RDS Endpoint: $RDS_ENDPOINT"
echo "EC2 Instance ID: $INSTANCE_ID"
echo "SSH Key: solar-management-key.pem"
echo "====================================="
```

---

### Phase 2: Application Deployment

#### Step 1: Connect to EC2

```bash
# SSH into EC2 instance
ssh -i solar-management-key.pem ec2-user@YOUR_ELASTIC_IP

# Verify Docker is installed
docker --version
docker-compose --version
```

#### Step 2: Clone Repository and Setup

```bash
# Navigate to application directory
cd /opt/solar-management

# Clone repository (or upload code via scp)
git clone https://github.com/your-org/solar-management-system.git .

# OR if uploading from local:
# From your local machine:
# scp -i solar-management-key.pem -r /path/to/local/project/* ec2-user@YOUR_ELASTIC_IP:/opt/solar-management/
```

#### Step 3: Create Environment Files

```bash
# Create production environment file
cat > .env.prod <<EOF
EC2_PUBLIC_IP=YOUR_ELASTIC_IP
RDS_ENDPOINT=YOUR_RDS_ENDPOINT
DB_PASSWORD=YOUR_RDS_PASSWORD
KEYCLOAK_ADMIN_PASSWORD=YOUR_KEYCLOAK_PASSWORD
GOOGLE_MAPS_API_KEY=YOUR_GOOGLE_MAPS_KEY
EOF

# Create staging environment file
cat > .env.staging <<EOF
EC2_PUBLIC_IP=YOUR_ELASTIC_IP
RDS_ENDPOINT=YOUR_RDS_ENDPOINT
DB_PASSWORD=YOUR_RDS_PASSWORD
KEYCLOAK_ADMIN_PASSWORD=YOUR_KEYCLOAK_PASSWORD
GOOGLE_MAPS_API_KEY=YOUR_GOOGLE_MAPS_KEY
EOF

# Secure the environment files
chmod 600 .env.prod .env.staging
```

#### Step 4: Deploy Production Environment

```bash
# Deploy production
./deploy.sh prod

# Monitor deployment
docker-compose -f docker-compose.prod.yml logs -f

# Wait for all containers to be healthy (Ctrl+C to exit logs)
```

#### Step 5: Deploy Staging Environment (Optional)

```bash
# Deploy staging
./deploy.sh staging

# Monitor deployment
docker-compose -f docker-compose.staging.yml logs -f
```

#### Step 6: Verify Deployment

```bash
# Check running containers
docker ps

# Test endpoints
curl http://YOUR_ELASTIC_IP/                  # Frontend
curl http://YOUR_ELASTIC_IP:8080/             # Backend
curl http://YOUR_ELASTIC_IP:8180/             # Keycloak
```

---

## Post-Deployment Configuration

### Configure Keycloak

1. **Access Keycloak Admin Console**:
   - URL: `http://YOUR_ELASTIC_IP:8180`
   - Username: `admin`
   - Password: (from KEYCLOAK_ADMIN_PASSWORD)

2. **Import Realm** (if realm-export.json exists):
   - Realm Settings → Import → Select realm-export.json

3. **OR Configure Manually**:
   - Create realm: `solar-management`
   - Create client: `solar-frontend`
     - Redirect URIs: `http://YOUR_ELASTIC_IP/*`
     - Web origins: `http://YOUR_ELASTIC_IP`
   - Create client: `solar-backend`
   - Create users (admin, technicians)
   - Assign roles (ADMIN, MANAGER, TECHNICIAN, ASSISTANT)

### Verify Database Connection

```bash
# Connect to RDS from EC2
psql -h YOUR_RDS_ENDPOINT -U postgres -d solar_management_prod

# Check tables (Spring Boot should have created them)
\dt

# Exit
\q
```

---

## Accessing the Application

### Production URLs

Replace `YOUR_ELASTIC_IP` with your actual Elastic IP:

- **Frontend**: `http://YOUR_ELASTIC_IP/`
- **Backend API**: `http://YOUR_ELASTIC_IP:8080/api`
- **Keycloak**: `http://YOUR_ELASTIC_IP:8180`
- **Keycloak Admin**: `http://YOUR_ELASTIC_IP:8180/admin`

### Staging URLs

- **Frontend**: `http://YOUR_ELASTIC_IP:3001/`
- **Backend API**: `http://YOUR_ELASTIC_IP:8081/api`
- **Keycloak**: `http://YOUR_ELASTIC_IP:8181`
- **Keycloak Admin**: `http://YOUR_ELASTIC_IP:8181/admin`

### SSH Access

```bash
ssh -i solar-management-key.pem ec2-user@YOUR_ELASTIC_IP
```

---

## Monitoring and Maintenance

### CloudWatch Monitoring

Set up CloudWatch alarms (via AWS Console):

1. **EC2 CPU Utilization** > 80%
2. **RDS CPU Utilization** > 80%
3. **RDS Free Storage Space** < 2GB
4. **RDS Database Connections** > 80

### View Logs

```bash
# Production logs
docker-compose -f docker-compose.prod.yml logs -f

# Specific service
docker-compose -f docker-compose.prod.yml logs -f backend

# Last 100 lines
docker-compose -f docker-compose.prod.yml logs --tail 100

# Staging logs
docker-compose -f docker-compose.staging.yml logs -f
```

### System Updates

```bash
# Update OS packages
sudo yum update -y

# Update Docker images
cd /opt/solar-management
docker-compose -f docker-compose.prod.yml pull
docker-compose -f docker-compose.prod.yml up -d

# Restart containers
docker-compose -f docker-compose.prod.yml restart
```

### Database Backups

RDS automated backups are enabled with 7-day retention. To create manual snapshot:

```bash
aws rds create-db-snapshot \
  --db-instance-identifier solar-rds \
  --db-snapshot-identifier solar-rds-manual-$(date +%Y%m%d-%H%M%S)
```

---

## Troubleshooting

### Issue: Container Out of Memory

```bash
# Check memory usage
free -h
docker stats

# Solution: Stop staging when not needed
docker-compose -f docker-compose.staging.yml down
```

### Issue: Cannot Connect to RDS

```bash
# Test connectivity
telnet YOUR_RDS_ENDPOINT 5432

# Check security group allows EC2
aws ec2 describe-security-groups --group-ids YOUR_RDS_SG_ID

# Verify RDS is running
aws rds describe-db-instances --db-instance-identifier solar-rds
```

### Issue: Application Not Starting

```bash
# Check container logs
docker-compose -f docker-compose.prod.yml logs backend

# Check environment variables
cat .env.prod

# Restart containers
docker-compose -f docker-compose.prod.yml restart
```

### Issue: Disk Space Full

```bash
# Check disk usage
df -h

# Clean Docker
docker system prune -a --volumes

# Clean logs
sudo journalctl --vacuum-time=7d
```

---

## Scaling Options

### When to Scale

- CPU consistently > 70%
- Memory consistently > 80%
- Response times degrading
- Free tier expiring

### Option 1: Upgrade Instance Types

```bash
# Stop instance
aws ec2 stop-instances --instance-ids YOUR_INSTANCE_ID

# Modify instance type
aws ec2 modify-instance-attribute \
  --instance-id YOUR_INSTANCE_ID \
  --instance-type t3.small

# Start instance
aws ec2 start-instances --instance-ids YOUR_INSTANCE_ID
```

### Option 2: Add Load Balancer

- Create Application Load Balancer
- Launch additional EC2 instances in Auto Scaling Group
- Use Multi-AZ RDS

### Option 3: Move to ECS Fargate

- Containerized deployment with better orchestration
- Automatic scaling
- No server management

---

## Security Best Practices

1. **Rotate Credentials** regularly
2. **Enable MFA** for AWS account
3. **Use AWS Secrets Manager** for sensitive data
4. **Keep systems updated** with security patches
5. **Monitor CloudWatch** alarms
6. **Regular backups** of RDS
7. **Implement HTTPS** when adding custom domain
8. **Review security groups** periodically

---

## Support and Additional Resources

- **AWS Free Tier**: https://aws.amazon.com/free
- **EC2 Documentation**: https://docs.aws.amazon.com/ec2
- **RDS Documentation**: https://docs.aws.amazon.com/rds
- **Docker Compose**: https://docs.docker.com/compose
- **Keycloak**: https://www.keycloak.org/documentation

---

**Deployment Date**: ___________
**Deployed By**: ___________
**Elastic IP**: ___________
**RDS Endpoint**: ___________
