#!/bin/bash
# EC2 User Data Script for Solar Management System
# This script runs once when the EC2 instance is first launched

# Update system packages
yum update -y

# Install Docker
yum install -y docker git

# Start Docker service
systemctl start docker
systemctl enable docker

# Add ec2-user to docker group (allows running docker without sudo)
usermod -aG docker ec2-user

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Create application directory
mkdir -p /opt/solar-management
chown ec2-user:ec2-user /opt/solar-management

# Install PostgreSQL client (for database management)
yum install -y postgresql15

# Configure timezone
timedatectl set-timezone Australia/Adelaide

# Enable automatic security updates
yum install -y yum-cron
systemctl enable yum-cron
systemctl start yum-cron
