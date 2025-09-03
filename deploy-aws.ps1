# AWS Deployment Script for NFL Pickem (Windows PowerShell)
# This script deploys your Spring Boot application to AWS Free Tier

param(
    [string]$StackName = "nfl-pickem-stack",
    [string]$Region = "us-east-1",
    [string]$TemplateFile = "aws-deployment.yml",
    [string]$KeyPairName = "your-key-pair-name"
)

Write-Host "🚀 Starting AWS deployment for NFL Pickem..." -ForegroundColor Green

# Check if AWS CLI is installed
try {
    $awsVersion = aws --version 2>$null
    if (-not $awsVersion) {
        Write-Host "❌ AWS CLI is not installed. Please install it first:" -ForegroundColor Red
        Write-Host "   https://aws.amazon.com/cli/" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "✅ AWS CLI found: $awsVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS CLI is not installed. Please install it first:" -ForegroundColor Red
    Write-Host "   https://aws.amazon.com/cli/" -ForegroundColor Yellow
    exit 1
}

# Check if AWS credentials are configured
try {
    $callerIdentity = aws sts get-caller-identity 2>$null
    if (-not $callerIdentity) {
        Write-Host "❌ AWS credentials not configured. Please run:" -ForegroundColor Red
        Write-Host "   aws configure" -ForegroundColor Yellow
        exit 1
    }
    Write-Host "✅ AWS credentials configured" -ForegroundColor Green
} catch {
    Write-Host "❌ AWS credentials not configured. Please run:" -ForegroundColor Red
    Write-Host "   aws configure" -ForegroundColor Yellow
    exit 1
}

Write-Host "📍 Deploying to region: $Region" -ForegroundColor Cyan
Write-Host "🏗️  Stack name: $StackName" -ForegroundColor Cyan

# Check if stack already exists
try {
    $existingStack = aws cloudformation describe-stacks --stack-name $StackName --region $Region 2>$null
    if ($existingStack) {
        Write-Host "📝 Updating existing stack..." -ForegroundColor Yellow
        aws cloudformation update-stack `
            --stack-name $StackName `
            --template-body file://$TemplateFile `
            --parameters ParameterKey=KeyName,ParameterValue=$KeyPairName `
            --capabilities CAPABILITY_IAM `
            --region $Region
        
        Write-Host "⏳ Waiting for stack update to complete..." -ForegroundColor Yellow
        aws cloudformation wait stack-update-complete `
            --stack-name $StackName `
            --region $Region
    }
} catch {
    Write-Host "🆕 Creating new stack..." -ForegroundColor Yellow
    aws cloudformation create-stack `
        --stack-name $StackName `
        --template-body file://$TemplateFile `
        --parameters ParameterKey=KeyName,ParameterValue=$KeyPairName `
        --capabilities CAPABILITY_IAM `
        --region $Region
    
    Write-Host "⏳ Waiting for stack creation to complete..." -ForegroundColor Yellow
    aws cloudformation wait stack-create-complete `
        --stack-name $StackName `
        --region $Region
}

# Get stack outputs
Write-Host "📊 Getting stack outputs..." -ForegroundColor Cyan
$EC2_IP = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`EC2PublicIP`].OutputValue' `
    --output text

$RDS_ENDPOINT = aws cloudformation describe-stacks `
    --stack-name $StackName `
    --region $Region `
    --query 'Stacks[0].Outputs[?OutputKey==`RDSEndpoint`].OutputValue' `
    --output text

Write-Host "✅ Deployment completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "🌐 Application URL: http://$EC2_IP" -ForegroundColor Green
Write-Host "🗄️  Database Endpoint: $RDS_ENDPOINT" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Next steps:" -ForegroundColor Cyan
Write-Host "1. Wait 5-10 minutes for the application to fully deploy" -ForegroundColor White
Write-Host "2. Test your application: http://$EC2_IP" -ForegroundColor White
Write-Host "3. Test The Odds API endpoints: http://$EC2_IP/api/admin/odds/test-connectivity" -ForegroundColor White
Write-Host "4. Check deployment logs in AWS Console" -ForegroundColor White
Write-Host ""
Write-Host "🔑 To SSH into your instance:" -ForegroundColor Cyan
Write-Host "   ssh -i your-key-pair.pem ec2-user@$EC2_IP" -ForegroundColor White
