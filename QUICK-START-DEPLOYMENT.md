# Quick Start Deployment Guide

## Fastest Way to Deploy (Recommended for Testing)

### Option 1: Frontend Only on S3 (5 minutes)

This deploys just the frontend. Backend stays on your local machine or existing server.

```bash
# 1. Install AWS CLI
brew install awscli  # macOS
# or download from https://aws.amazon.com/cli/

# 2. Configure AWS credentials
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter region: ap-southeast-2

# 3. Build and deploy frontend
cd frontend
npm run build
./deploy-s3.sh

# That's it! Your frontend is live on S3
```

**Result**: Frontend on S3, Backend on your current server

---

### Option 2: Full Stack on AWS EC2 (30 minutes)

Deploy everything (Frontend + Backend + Database) on a single EC2 instance.

#### Step 1: Create EC2 Instance

1. Go to [AWS EC2 Console](https://console.aws.amazon.com/ec2/)
2. Click "Launch Instance"
3. Choose:
   - Name: solar-management-server
   - AMI: Ubuntu Server 22.04
   - Instance type: t3.medium (or t3.small for testing)
   - Key pair: Create new or select existing
4. Configure Security Group:
   - SSH (22) - Your IP only
   - HTTP (80) - Anywhere
   - HTTPS (443) - Anywhere
   - Custom TCP (8080) - Anywhere (or your IP for testing)
   - Custom TCP (3000) - Anywhere (or your IP for testing)
5. Click "Launch Instance"

#### Step 2: Connect to EC2

```bash
# Download your key pair (yourkey.pem)
chmod 400 yourkey.pem

# Connect
ssh -i yourkey.pem ubuntu@YOUR_EC2_PUBLIC_IP
```

#### Step 3: Install Dependencies on EC2

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Logout and login again
exit
# SSH back in
```

#### Step 4: Copy Your Project

On your local machine:
```bash
# Copy project to EC2
scp -i yourkey.pem -r /Users/frederick/Documents/work/solar-management-system ubuntu@YOUR_EC2_PUBLIC_IP:~/
```

#### Step 5: Run on EC2

```bash
# SSH back into EC2
ssh -i yourkey.pem ubuntu@YOUR_EC2_PUBLIC_IP

# Navigate to project
cd solar-management-system

# Start everything
docker-compose up -d

# Check status
docker-compose ps
```

#### Step 6: Configure Domain (Optional)

1. Point your domain to EC2 public IP in DNS settings
2. Install nginx as reverse proxy:

```bash
sudo apt install -y nginx

# Configure nginx
sudo nano /etc/nginx/sites-available/solar-management
```

Add this configuration:
```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # Frontend
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # Backend API
    location /api {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

Enable the site:
```bash
sudo ln -s /etc/nginx/sites-available/solar-management /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

#### Step 7: Enable HTTPS with Let's Encrypt

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```

---

### Option 3: Hybrid Approach (15 minutes)

Frontend on S3/CloudFront, Backend on EC2.

1. **Deploy Frontend to S3**: Follow Option 1
2. **Deploy Backend to EC2**:
   ```bash
   # On EC2, only run backend services
   docker-compose up -d postgres keycloak backend
   ```
3. **Update Frontend Config**:
   - Update `REACT_APP_API_URL` to point to EC2 public IP
   - Rebuild and redeploy frontend

---

## Cost Comparison

| Option | Monthly Cost | Best For |
|--------|-------------|----------|
| Option 1 (S3 Only) | $5-10 | Testing frontend, backend stays local |
| Option 2 (EC2 Full Stack) | $20-40 | Small production, single server |
| Option 3 (Hybrid) | $25-50 | Medium production, separate concerns |
| Full AWS (see DEPLOYMENT.md) | $85-145 | Production, high availability |

---

## Which Option Should I Choose?

- **Just testing?** → Option 1 (S3 Frontend only)
- **Small business/startup?** → Option 2 (EC2 Full Stack)
- **Growing business?** → Option 3 (Hybrid)
- **Enterprise/Scale?** → Full AWS (see DEPLOYMENT.md)

---

## Next Steps After Deployment

1. **Set up backups**:
   ```bash
   # On EC2, create daily backup script
   docker exec postgres pg_dump -U postgres > backup-$(date +%Y%m%d).sql
   ```

2. **Set up monitoring**:
   - Install CloudWatch agent on EC2
   - Set up uptime monitoring (UptimeRobot, Pingdom)

3. **Secure your deployment**:
   - Change default passwords
   - Restrict security group rules
   - Enable AWS CloudTrail
   - Set up WAF (Web Application Firewall)

4. **Set up CI/CD**:
   - Use GitHub Actions to auto-deploy on push
   - See `.github/workflows/` examples online

---

## Troubleshooting

### Can't connect to EC2
- Check security group allows your IP
- Verify key pair permissions: `chmod 400 yourkey.pem`
- Check EC2 instance is running

### Docker not working
- Make sure you logged out and back in after adding user to docker group
- Try `sudo docker-compose up -d` if permission issues

### Frontend can't reach backend
- Check `REACT_APP_API_URL` in frontend
- Verify backend is running: `curl http://localhost:8080/actuator/health`
- Check security group allows port 8080

### Database connection errors
- Verify postgres container is running
- Check database credentials in environment variables
- Look at logs: `docker-compose logs postgres`

---

## Support & Resources

- Full deployment guide: See `DEPLOYMENT.md`
- AWS Free Tier: https://aws.amazon.com/free/
- EC2 Pricing: https://aws.amazon.com/ec2/pricing/
- Docker Documentation: https://docs.docker.com/
