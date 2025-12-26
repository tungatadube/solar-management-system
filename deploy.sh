#!/bin/bash
# Deployment script for Solar Management System on AWS
# Usage: ./deploy.sh [prod|staging]

set -e  # Exit on error

ENV=${1:-prod}

echo "========================================="
echo "Solar Management System Deployment"
echo "Environment: $ENV"
echo "========================================="
echo ""

if [ "$ENV" == "prod" ]; then
  echo "Deploying PRODUCTION environment..."
  echo ""

  # Pull latest images
  echo "Pulling latest Docker images..."
  docker-compose -f docker-compose.prod.yml --env-file .env.prod pull

  # Stop existing containers
  echo "Stopping existing containers..."
  docker-compose -f docker-compose.prod.yml --env-file .env.prod down

  # Build and start containers
  echo "Building and starting containers..."
  docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d --build

elif [ "$ENV" == "staging" ]; then
  echo "Deploying STAGING environment..."
  echo ""

  # Pull latest images
  echo "Pulling latest Docker images..."
  docker-compose -f docker-compose.staging.yml --env-file .env.staging pull

  # Stop existing containers
  echo "Stopping existing containers..."
  docker-compose -f docker-compose.staging.yml --env-file .env.staging down

  # Build and start containers
  echo "Building and starting containers..."
  docker-compose -f docker-compose.staging.yml --env-file .env.staging up -d --build

else
  echo "Error: Invalid environment '$ENV'"
  echo "Usage: ./deploy.sh [prod|staging]"
  exit 1
fi

echo ""
echo "Deployment complete!"
echo ""
echo "Running containers:"
docker ps --filter "name=solar-*" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "To view logs:"
if [ "$ENV" == "prod" ]; then
  echo "  docker-compose -f docker-compose.prod.yml logs -f"
else
  echo "  docker-compose -f docker-compose.staging.yml logs -f"
fi
echo ""
