# DigitalOcean Deployment Guide for NFL Pickem

## 🚀 **Overview**

This guide provides a cost-effective alternative to AWS Free Tier using DigitalOcean. DigitalOcean offers:
- **Clean IP addresses** (solves The Odds API 403 errors)
- **Professional hosting** infrastructure
- **Predictable pricing** with no hidden fees
- **Easy deployment** with Docker

## 💰 **Cost Breakdown**

### **Monthly Costs:**
- **Basic Droplet (1GB RAM, 1 vCPU)**: $12/month
- **Managed PostgreSQL Database**: $15/month
- **Data Transfer**: $0.01/GB (first 1TB free)
- **Total**: **$27/month**

### **Why DigitalOcean is Cost-Effective:**
- **No free tier restrictions** - same price for everyone
- **Professional IP reputation** - APIs trust their IPs
- **Managed database** - no maintenance overhead
- **Predictable billing** - no surprise charges

## 🔧 **Prerequisites**

### **1. DigitalOcean Account**
1. **Create account** at [digitalocean.com](https://digitalocean.com)
2. **Add payment method** (credit card required)
3. **Verify account** (may take 24-48 hours)

### **2. Install Docker (Local)**
```bash
# Windows
winget install -e --id Docker.DockerDesktop

# macOS
brew install --cask docker

# Linux
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
```

### **3. Install DigitalOcean CLI (Optional)**
```bash
# Windows
winget install -e --id DigitalOcean.Doctl

# macOS
brew install doctl

# Linux
snap install doctl
```

## 🚀 **Deployment Steps**

### **Step 1: Create DigitalOcean Droplet**
1. **Go to DigitalOcean Console** → **Create** → **Droplets**
2. **Choose image**: Ubuntu 22.04 LTS
3. **Choose size**: Basic ($12/month) - 1GB RAM, 1 vCPU
4. **Choose datacenter**: NYC3 (closest to you)
5. **Authentication**: SSH Key (recommended) or Password
6. **Finalize**: Create Droplet

### **Step 2: Create Managed Database**
1. **Go to Databases** → **Create Database Cluster**
2. **Choose engine**: PostgreSQL
3. **Choose version**: 15
4. **Choose size**: Basic ($15/month) - 1GB RAM, 1 vCPU
5. **Choose datacenter**: Same as Droplet
6. **Create database**

### **Step 3: Build Your Application**
```bash
# Build the JAR file
mvn clean package -DskipTests

# Verify the JAR was created
ls -la target/pickem-0.0.1-SNAPSHOT.jar
```

### **Step 4: Deploy to Droplet**
```bash
# SSH into your Droplet
ssh root@[YOUR-DROPLET-IP]

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Install Docker Compose
sudo apt-get update
sudo apt-get install -y docker-compose

# Create application directory
mkdir -p /opt/nfl-pickem
cd /opt/nfl-pickem

# Upload your JAR file (from your local machine)
scp target/pickem-0.0.1-SNAPSHOT.jar root@[YOUR-DROPLET-IP]:/opt/nfl-pickem/

# Upload Docker Compose file
scp digitalocean-deployment.yml root@[YOUR-DROPLET-IP]:/opt/nfl-pickem/docker-compose.yml

# Create environment file
cat > .env << EOF
THEODDS_API_KEY=04f5a0643a9fe4b5b3b390c8037ae885
DB_HOST=[YOUR-DATABASE-HOST]
DB_NAME=nflpickem
DB_USERNAME=[YOUR-DATABASE-USER]
DB_PASSWORD=[YOUR-DATABASE-PASSWORD]
EOF

# Start the application
docker-compose up -d
```

### **Step 5: Configure Nginx**
```bash
# Create nginx configuration
cat > nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream nfl-pickem {
        server nfl-pickem:8080;
    }

    server {
        listen 80;
        server_name _;

        location / {
            proxy_pass http://nfl-pickem;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
    }
}
EOF

# Restart nginx
docker-compose restart nginx
```

## 🌐 **Post-Deployment Setup**

### **1. Test Your Application**
```bash
# Health check
curl http://[YOUR-DROPLET-IP]/actuator/health

# Test The Odds API
curl http://[YOUR-DROPLET-IP]/api/admin/odds/test-connectivity

# Check application logs
docker-compose logs -f nfl-pickem
```

### **2. Set Up Domain (Optional)**
1. **Point your domain** to the Droplet IP
2. **Configure SSL** with Let's Encrypt
3. **Update nginx** configuration for HTTPS

### **3. Monitor Performance**
```bash
# Check resource usage
docker stats

# Check disk space
df -h

# Check memory usage
free -h
```

## 🗄️ **Database Migration**

### **1. Export from Railway**
```bash
# Get connection details from Railway
pg_dump -h [RAILWAY-HOST] -U [USERNAME] -d [DATABASE] > backup.sql
```

### **2. Import to DigitalOcean**
```bash
# Get connection details from DigitalOcean Console
psql -h [DIGITALOCEAN-HOST] -U [USERNAME] -d nflpickem < backup.sql
```

## 🔍 **Troubleshooting**

### **1. Common Issues**
- **Port 8080 not accessible**: Check firewall settings
- **Database connection failed**: Verify database credentials
- **Application not starting**: Check Docker logs

### **2. Useful Commands**
```bash
# Check application status
docker-compose ps

# View logs
docker-compose logs nfl-pickem

# Restart services
docker-compose restart

# Update application
docker-compose pull
docker-compose up -d
```

## 🔒 **Security Considerations**

### **1. Firewall Setup**
```bash
# Configure UFW firewall
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP
ufw allow 443/tcp   # HTTPS
ufw enable
```

### **2. SSH Security**
- **Use SSH keys** instead of passwords
- **Change default SSH port** (optional)
- **Limit SSH access** to your IP

### **3. Database Security**
- **Use strong passwords**
- **Limit database access** to Droplet IP only
- **Regular backups**

## 📊 **Performance & Scaling**

### **1. Current Limitations**
- **1GB RAM**: Sufficient for development/testing
- **1 vCPU**: Good for moderate traffic
- **25GB SSD**: Plenty for application + logs

### **2. Scaling Options**
- **Vertical**: Upgrade to larger Droplet ($24/month for 2GB RAM)
- **Horizontal**: Add more Droplets behind load balancer
- **Database**: Upgrade to larger database instance

### **3. Cost Optimization**
- **Reserved Droplets**: 12-month commitment for 20% discount
- **Volume discounts**: 5+ Droplets get additional savings
- **Monitoring**: Use DigitalOcean's built-in monitoring

## 🔄 **Rollback Plan**

### **1. Keep Railway Running**
- **Don't delete** Railway resources yet
- **Test DigitalOcean thoroughly** before switching
- **Verify The Odds API** access works

### **2. DNS Switchover**
- **Update DNS** to point to DigitalOcean IP
- **Monitor** for any issues
- **Keep Railway** as backup for 24-48 hours

### **3. If Issues Arise**
- **Switch DNS back** to Railway
- **Investigate** DigitalOcean issues
- **Fix and retry** migration

## 📞 **Support Resources**

### **1. DigitalOcean Documentation**
- [Droplets User Guide](https://docs.digitalocean.com/products/droplets/)
- [Databases User Guide](https://docs.digitalocean.com/products/databases/)
- [Docker on DigitalOcean](https://docs.digitalocean.com/tutorials/docker/)

### **2. Community Support**
- [DigitalOcean Community](https://www.digitalocean.com/community/)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/digitalocean)
- [Reddit r/digitalocean](https://www.reddit.com/r/digitalocean/)

### **3. Cost Management**
- [DigitalOcean Pricing](https://www.digitalocean.com/pricing/)
- [Billing & Usage](https://cloud.digitalocean.com/account/billing)
- [Usage Alerts](https://cloud.digitalocean.com/account/alerts)

## 🎯 **Expected Results**

### **1. Better API Access**
- **Clean IP reputation** with The Odds API
- **Professional hosting** infrastructure
- **Reliable connectivity**

### **2. Improved Performance**
- **Dedicated resources** (not shared hosting)
- **Optimized Java runtime**
- **Managed database** with automatic backups

### **3. Cost Benefits**
- **Predictable pricing** - no hidden fees
- **Professional infrastructure** at reasonable cost
- **Easy scaling** as your needs grow

## 🆚 **DigitalOcean vs AWS Comparison**

| Feature | DigitalOcean | AWS (No Free Tier) |
|---------|--------------|-------------------|
| **Monthly Cost** | $27/month | $22/month |
| **Setup Complexity** | Simple | Complex |
| **IP Reputation** | Excellent | Excellent |
| **Support** | Community + Paid | Community + Paid |
| **Scaling** | Easy | Advanced |
| **Learning Curve** | Low | High |

---

**DigitalOcean is perfect if you want professional hosting without AWS complexity, and it will definitely solve your The Odds API access issues!**
