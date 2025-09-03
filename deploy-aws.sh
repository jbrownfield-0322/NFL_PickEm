#!/bin/bash

# AWS Deployment Script for NFL Pickem
# This script deploys your Spring Boot application to AWS Free Tier

set -e

echo "🚀 Starting AWS deployment for NFL Pickem..."

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI is not installed. Please install it first:"
    echo "   https://aws.amazon.com/cli/"
    exit 1
fi

# Check if AWS credentials are configured
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS credentials not configured. Please run:"
    echo "   aws configure"
    exit 1
fi

# Variables
STACK_NAME="nfl-pickem-stack"
REGION="us-east-1"  # Free Tier region
TEMPLATE_FILE="aws-deployment.yml"

echo "📍 Deploying to region: $REGION"
echo "🏗️  Stack name: $STACK_NAME"

# Check if stack already exists
if aws cloudformation describe-stacks --stack-name $STACK_NAME --region $REGION &> /dev/null; then
    echo "📝 Updating existing stack..."
    aws cloudformation update-stack \
        --stack-name $STACK_NAME \
        --template-body file://$TEMPLATE_FILE \
        --parameters ParameterKey=KeyName,ParameterValue=your-key-pair-name \
        --capabilities CAPABILITY_IAM \
        --region $REGION
    
    echo "⏳ Waiting for stack update to complete..."
    aws cloudformation wait stack-update-complete \
        --stack-name $STACK_NAME \
        --region $REGION
else
    echo "🆕 Creating new stack..."
    aws cloudformation create-stack \
        --stack-name $STACK_NAME \
        --template-body file://$TEMPLATE_FILE \
        --parameters ParameterKey=KeyName,ParameterValue=your-key-pair-name \
        --capabilities CAPABILITY_IAM \
        --region $REGION
    
    echo "⏳ Waiting for stack creation to complete..."
    aws cloudformation wait stack-create-complete \
        --stack-name $STACK_NAME \
        --region $REGION
fi

# Get stack outputs
echo "📊 Getting stack outputs..."
EC2_IP=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`EC2PublicIP`].OutputValue' \
    --output text)

RDS_ENDPOINT=$(aws cloudformation describe-stacks \
    --stack-name $STACK_NAME \
    --region $REGION \
    --query 'Stacks[0].Outputs[?OutputKey==`RDSEndpoint`].OutputValue' \
    --output text)

echo "✅ Deployment completed successfully!"
echo ""
echo "🌐 Application URL: http://$EC2_IP"
echo "🗄️  Database Endpoint: $RDS_ENDPOINT"
echo ""
echo "📋 Next steps:"
echo "1. Wait 5-10 minutes for the application to fully deploy"
echo "2. Test your application: http://$EC2_IP"
echo "3. Test The Odds API endpoints: http://$EC2_IP/api/admin/odds/test-connectivity"
echo "4. Check deployment logs in AWS Console"
echo ""
echo "🔑 To SSH into your instance:"
echo "   ssh -i your-key-pair.pem ec2-user@$EC2_IP"
