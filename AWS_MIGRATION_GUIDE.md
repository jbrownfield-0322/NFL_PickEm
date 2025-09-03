# AWS Migration Guide for NFL Pickem

## 🚀 **Overview**

This guide will help you migrate your NFL Pickem project from Railway to AWS Free Tier. The migration includes:

- **EC2 Instance** (t2.micro - Free Tier eligible)
- **RDS PostgreSQL** (db.t3.micro - Free Tier eligible)
- **VPC with security groups**
- **Auto-deployment from GitHub**
- **Nginx reverse proxy**

## 💰 **Cost Breakdown**

### **Free Tier (First 12 Months)**
- **EC2 t2.micro**: $0/month (750 hours)
- **RDS db.t3.micro**: $0/month (750 hours)
- **Data Transfer**: $0/month (15GB)
- **Total**: ~$0.50/month (minimal charges)

### **After Free Tier**
- **EC2 t2.micro**: $8.47/month
- **RDS db.t3.micro**: $13.68/month
- **Total**: ~$22/month

## 🔧 **Prerequisites**

### **1. AWS Account Setup**
1. **Create AWS Account** at [aws.amazon.com](https://aws.amazon.com)
2. **Verify account** (credit card required, but won't be charged during free tier)
3. **Set up billing alerts** to avoid unexpected charges

### **2. AWS CLI Installation**
```bash
# Windows (PowerShell)
winget install -e --id Amazon.AWSCLI

# macOS
brew install awscli

# Linux
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

### **3. AWS Credentials Configuration**
```bash
aws configure
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter your default region (us-east-1 for free tier)
# Enter your output format (json)
```

### **4. EC2 Key Pair Creation**
1. **Go to AWS Console** → **EC2** → **Key Pairs**
2. **Create Key Pair** named `nfl-pickem-key`
3. **Download the .pem file** and save it securely
4. **Note the key pair name** for deployment

## 🚀 **Deployment Steps**

### **Step 1: Update Configuration**
1. **Edit `deploy-aws.ps1`** (Windows) or `deploy-aws.sh` (Linux/Mac)
2. **Change `your-key-pair-name`** to your actual key pair name
3. **Verify region** is `us-east-1` (free tier region)

### **Step 2: Deploy Infrastructure**
```bash
# Windows PowerShell
.\deploy-aws.ps1

# Linux/Mac
chmod +x deploy-aws.sh
./deploy-aws.sh
```

### **Step 3: Wait for Deployment**
- **Stack creation**: 10-15 minutes
- **EC2 setup**: 5-10 minutes
- **Application deployment**: 5-10 minutes
- **Total time**: 20-35 minutes

## 🌐 **Post-Deployment Setup**

### **1. Get Your Application URL**
The deployment script will output:
```
🌐 Application URL: http://[EC2-IP-ADDRESS]
🗄️  Database Endpoint: [RDS-ENDPOINT]
```

### **2. Set Environment Variables**
SSH into your EC2 instance:
```bash
ssh -i nfl-pickem-key.pem ec2-user@[EC2-IP-ADDRESS]
```

Set environment variables:
```bash
sudo systemctl edit nfl-pickem
```

Add these lines:
```ini
[Service]
Environment="THEODDS_API_KEY=04f5a0643a9fe4b5b3b390c8037ae885"
Environment="DB_HOST=[RDS-ENDPOINT]"
Environment="DB_NAME=nflpickem"
Environment="DB_USERNAME=admin"
Environment="DB_PASSWORD=[PASSWORD-FROM-SECRETS-MANAGER]"
```

Restart the service:
```bash
sudo systemctl restart nfl-pickem
```

### **3. Test Your Application**
1. **Health Check**: `http://[EC2-IP]/actuator/health`
2. **Main App**: `http://[EC2-IP]/api/games`
3. **Admin Endpoints**: `http://[EC2-IP]/api/admin/odds/test-connectivity`

## 🗄️ **Database Migration**

### **1. Export from Railway**
```bash
# Get connection details from Railway
pg_dump -h [RAILWAY-HOST] -U [USERNAME] -d [DATABASE] > backup.sql
```

### **2. Import to AWS RDS**
```bash
# Get connection details from AWS Secrets Manager
aws secretsmanager get-secret-value --secret-id nfl-pickem-stack/db-password

# Import data
psql -h [RDS-ENDPOINT] -U admin -d nflpickem < backup.sql
```

## 🔍 **Monitoring & Troubleshooting**

### **1. Check Application Logs**
```bash
# SSH into EC2 instance
ssh -i nfl-pickem-key.pem ec2-user@[EC2-IP]

# Check application logs
sudo journalctl -u nfl-pickem -f

# Check nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### **2. Check AWS Console**
- **CloudFormation**: Stack status and events
- **EC2**: Instance status and logs
- **RDS**: Database status and metrics
- **CloudWatch**: Application metrics

### **3. Common Issues**
- **Port 8080 not accessible**: Check security groups
- **Database connection failed**: Check RDS security groups
- **Application not starting**: Check Java installation and logs

## 🔒 **Security Considerations**

### **1. Security Groups**
- **EC2**: SSH (22), HTTP (80), Application (8080)
- **RDS**: PostgreSQL (5432) from EC2 only

### **2. Network Security**
- **VPC**: Isolated network environment
- **Private Subnet**: RDS in private subnet
- **Public Subnet**: EC2 with public IP

### **3. Access Control**
- **SSH Key**: Secure key pair authentication
- **Database**: Strong password via Secrets Manager
- **IAM**: Minimal required permissions

## 📊 **Performance & Scaling**

### **1. Free Tier Limitations**
- **EC2**: 1 vCPU, 1GB RAM
- **RDS**: 1 vCPU, 1GB RAM
- **Storage**: 20GB EBS, 20GB RDS

### **2. Scaling Options**
- **Vertical**: Upgrade to larger instance types
- **Horizontal**: Add more EC2 instances
- **Database**: RDS read replicas

### **3. Cost Optimization**
- **Reserved Instances**: 1-3 year commitments
- **Spot Instances**: For non-critical workloads
- **S3**: Store static assets

## 🔄 **Rollback Plan**

### **1. Keep Railway Running**
- **Don't delete** Railway resources yet
- **Test AWS thoroughly** before switching
- **Verify The Odds API** access works

### **2. DNS Switchover**
- **Update DNS** to point to AWS IP
- **Monitor** for any issues
- **Keep Railway** as backup for 24-48 hours

### **3. If Issues Arise**
- **Switch DNS back** to Railway
- **Investigate** AWS issues
- **Fix and retry** migration

## 📞 **Support Resources**

### **1. AWS Documentation**
- [EC2 User Guide](https://docs.aws.amazon.com/ec2/)
- [RDS User Guide](https://docs.aws.amazon.com/rds/)
- [CloudFormation User Guide](https://docs.aws.amazon.com/cloudformation/)

### **2. Troubleshooting**
- [AWS Support Center](https://console.aws.amazon.com/support/)
- [AWS Forums](https://forums.aws.amazon.com/)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/amazon-web-services)

### **3. Cost Management**
- [AWS Cost Explorer](https://console.aws.amazon.com/costexplorer/)
- [Billing Alerts](https://console.aws.amazon.com/billing/)
- [Free Tier Usage](https://console.aws.amazon.com/billing/)

## 🎯 **Expected Results**

### **1. Better API Access**
- **Clean IP reputation** with The Odds API
- **Professional hosting** infrastructure
- **Reliable connectivity**

### **2. Improved Performance**
- **Dedicated resources** (not shared hosting)
- **Optimized Java runtime**
- **Professional database** management

### **3. Cost Benefits**
- **12 months free** to test everything
- **Predictable pricing** after free tier
- **Professional infrastructure** at reasonable cost

---

**Good luck with your AWS migration! This should resolve your The Odds API access issues.**
