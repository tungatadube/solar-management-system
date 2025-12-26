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
