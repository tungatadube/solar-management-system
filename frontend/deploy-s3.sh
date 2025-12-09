#!/bin/bash

# Configuration
BUCKET_NAME="solar-management-frontend"  # Change this to your desired bucket name
REGION="ap-southeast-2"  # Change to your preferred region
DISTRIBUTION_ID=""  # Will be filled after CloudFront creation

echo "🚀 Starting deployment to S3..."

# Step 1: Build the React app
echo "📦 Building React app..."
npm run build

# Step 2: Create S3 bucket (if it doesn't exist)
echo "🗄️  Creating S3 bucket..."
aws s3api create-bucket \
  --bucket $BUCKET_NAME \
  --region $REGION \
  --create-bucket-configuration LocationConstraint=$REGION \
  2>/dev/null || echo "Bucket already exists"

# Step 3: Enable static website hosting
echo "🌐 Enabling static website hosting..."
aws s3 website s3://$BUCKET_NAME/ \
  --index-document index.html \
  --error-document index.html

# Step 4: Set bucket policy for public read
echo "📝 Setting bucket policy..."
cat > /tmp/bucket-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::$BUCKET_NAME/*"
    }
  ]
}
EOF

aws s3api put-bucket-policy \
  --bucket $BUCKET_NAME \
  --policy file:///tmp/bucket-policy.json

# Step 5: Upload build files to S3
echo "⬆️  Uploading files to S3..."
aws s3 sync build/ s3://$BUCKET_NAME/ \
  --delete \
  --cache-control "max-age=31536000" \
  --exclude "*.html" \
  --exclude "service-worker.js"

# Upload HTML files with no-cache
aws s3 sync build/ s3://$BUCKET_NAME/ \
  --exclude "*" \
  --include "*.html" \
  --include "service-worker.js" \
  --cache-control "no-cache, no-store, must-revalidate"

# Step 6: Invalidate CloudFront cache (if distribution exists)
if [ ! -z "$DISTRIBUTION_ID" ]; then
  echo "🔄 Invalidating CloudFront cache..."
  aws cloudfront create-invalidation \
    --distribution-id $DISTRIBUTION_ID \
    --paths "/*"
fi

echo "✅ Deployment complete!"
echo "🌍 Website URL: http://$BUCKET_NAME.s3-website-$REGION.amazonaws.com"

# Instructions for CloudFront setup
if [ -z "$DISTRIBUTION_ID" ]; then
  echo ""
  echo "📌 Next steps:"
  echo "1. Create CloudFront distribution for HTTPS and better performance"
  echo "2. Add the distribution ID to this script"
  echo "3. Configure custom domain (optional)"
fi
