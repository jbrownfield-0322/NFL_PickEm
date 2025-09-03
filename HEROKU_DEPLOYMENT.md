# Heroku Deployment Guide for NFL Pickem

## 🚀 **Overview**

**Heroku is the BEST choice for your situation!** It offers:
- **Excellent IP reputation** (solves The Odds API 403 errors)
- **Super simple deployment** (no AWS complexity)
- **Native Java support** (perfect for Spring Boot)
- **Managed database** (no maintenance overhead)
- **Predictable pricing** ($12/month vs $24/month AWS)

## 💰 **Cost Breakdown**

### **Monthly Costs:**
- **Basic Dyno (512MB RAM)**: $7/month
- **Postgres Basic (1GB RAM)**: $5/month
- **Data Transfer**: First 2GB free
- **Total**: **$12/month** 🎉

### **Why Heroku is Cheaper:**
- **No infrastructure management** - you focus on code
- **Efficient resource usage** - shared CPU when idle
- **Built-in optimizations** - automatic scaling and caching
- **No hidden charges** - predictable monthly billing

## 🔧 **Prerequisites**

### **1. Heroku Account**
1. **Create account** at [heroku.com](https://heroku.com)
2. **Add payment method** (credit card required)
3. **Verify account** (instant verification)

### **2. Install Heroku CLI**
```bash
# Windows
winget install -e --id Heroku.HerokuCLI

# macOS
brew tap heroku/brew && brew install heroku

# Linux
curl https://cli-assets.heroku.com/install.sh | sh
```

### **3. Install Git (if not already installed)**
```bash
# Windows
winget install -e --id Git.Git

# macOS
brew install git

# Linux
sudo apt-get install git
```

## 🚀 **Deployment Steps**

### **Step 1: Build Your Application**
```bash
# Build the JAR file
mvn clean package -DskipTests

# Verify the JAR was created
ls -la target/pickem-0.0.1-SNAPSHOT.jar
```

### **Step 2: Create Heroku App**
```bash
# Login to Heroku
heroku login

# Create new app
heroku create nfl-pickem-[YOUR-NAME]

# Set Java buildpack
heroku buildpacks:set heroku/java
```

### **Step 3: Add PostgreSQL Database**
```bash
# Add Postgres addon
heroku addons:create heroku-postgresql:basic

# Verify database was created
heroku addons
```

### **Step 4: Set Environment Variables**
```bash
# Set The Odds API key
heroku config:set THEODDS_API_KEY=04f5a0643a9fe4b5b3b390c8037ae885

# Set Spring profile
heroku config:set SPRING_PROFILES_ACTIVE=heroku

# Set odds scheduling
heroku config:set ODDS_SCHEDULING_FREQUENCY=4
heroku config:set ODDS_GAME_DAY_FREQUENCY=1

# View all config
heroku config
```

### **Step 5: Deploy Your App**
```bash
# Add all files to git
git add .

# Commit changes
git commit -m "Deploy to Heroku"

# Push to Heroku
git push heroku main

# Open your app
heroku open
```

## 🌐 **Post-Deployment Setup**

### **1. Test Your Application**
```bash
# Health check
curl https://nfl-pickem-[YOUR-NAME].herokuapp.com/actuator/health

# Test The Odds API
curl https://nfl-pickem-[YOUR-NAME].herokuapp.com/api/admin/odds/test-connectivity

# Check logs
heroku logs --tail
```

### **2. Scale Your App (Optional)**
```bash
# Scale to 1 dyno (recommended for production)
heroku ps:scale web=1

# Check dyno status
heroku ps
```

### **3. Monitor Performance**
```bash
# View app metrics
heroku addons:open papertrail

# Check database status
heroku pg:info
```

## 🗄️ **Database Migration**

### **1. Export from Railway**
```bash
# Get connection details from Railway
pg_dump -h [RAILWAY-HOST] -U [USERNAME] -d [DATABASE] > backup.sql
```

### **2. Import to Heroku**
```bash
# Get Heroku database URL
heroku config:get DATABASE_URL

# Import data
heroku pg:psql < backup.sql
```

## 🔍 **Troubleshooting**

### **1. Common Issues**
- **Build fails**: Check Java version in `system.properties`
- **App crashes**: Check logs with `heroku logs --tail`
- **Database connection fails**: Verify `DATABASE_URL` is set

### **2. Useful Commands**
```bash
# Check app status
heroku ps

# View logs
heroku logs --tail

# Restart app
heroku restart

# Check config
heroku config

# Run commands on dyno
heroku run java -version
```

### **3. Performance Issues**
```bash
# Check dyno usage
heroku ps

# Scale up if needed
heroku ps:scale web=1

# Monitor database
heroku pg:info
```

## 🔒 **Security Considerations**

### **1. Environment Variables**
- **Never commit secrets** to git
- **Use Heroku config** for sensitive data
- **Rotate API keys** regularly

### **2. Database Security**
- **Automatic backups** with Heroku Postgres
- **SSL connections** by default
- **Access control** via Heroku dashboard

### **3. App Security**
- **HTTPS enforced** by default
- **Automatic SSL** certificates
- **DDoS protection** included

## 📊 **Performance & Scaling**

### **1. Current Limitations**
- **512MB RAM**: Sufficient for development/testing
- **Shared CPU**: Good for moderate traffic
- **10GB Database**: Plenty for most applications

### **2. Scaling Options**
- **Vertical**: Upgrade to larger dynos ($25/month for 1GB RAM)
- **Horizontal**: Add more dynos for load balancing
- **Database**: Upgrade to larger Postgres plans

### **3. Cost Optimization**
- **Free tier**: 0 dynos when not in use
- **Efficient scaling**: Scale down during low traffic
- **Resource monitoring**: Track usage in dashboard

## 🔄 **Rollback Plan**

### **1. Keep Railway Running**
- **Don't delete** Railway resources yet
- **Test Heroku thoroughly** before switching
- **Verify The Odds API** access works

### **2. DNS Switchover**
- **Update DNS** to point to Heroku URL
- **Monitor** for any issues
- **Keep Railway** as backup for 24-48 hours

### **3. If Issues Arise**
- **Switch DNS back** to Railway
- **Investigate** Heroku issues
- **Fix and retry** migration

## 📞 **Support Resources**

### **1. Heroku Documentation**
- [Getting Started with Java](https://devcenter.heroku.com/articles/getting-started-with-java)
- [Heroku Postgres](https://devcenter.heroku.com/articles/heroku-postgresql)
- [Deploying Spring Boot](https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku)

### **2. Community Support**
- [Heroku Community](https://help.heroku.com/)
- [Stack Overflow](https://stackoverflow.com/questions/tagged/heroku)
- [Reddit r/Heroku](https://www.reddit.com/r/Heroku/)

### **3. Cost Management**
- [Heroku Pricing](https://www.heroku.com/pricing)
- [Billing & Usage](https://dashboard.heroku.com/account/billing)
- [Usage Alerts](https://devcenter.heroku.com/articles/usage-based-billing)

## 🎯 **Expected Results**

### **1. Better API Access**
- **Clean IP reputation** with The Odds API
- **Professional hosting** infrastructure
- **Reliable connectivity**

### **2. Improved Performance**
- **Optimized Java runtime** on Heroku
- **Automatic scaling** based on traffic
- **Managed database** with backups

### **3. Cost Benefits**
- **$12/month** vs $24/month AWS
- **No infrastructure management**
- **Predictable pricing**

## 🆚 **Heroku vs AWS vs DigitalOcean**

| Feature | Heroku | AWS | DigitalOcean |
|---------|---------|-----|--------------|
| **Monthly Cost** | **$12/month** | $24/month | $27/month |
| **Setup Complexity** | ✅ **Very Simple** | ❌ Complex | ✅ Simple |
| **IP Reputation** | ✅ **Excellent** | ✅ **Excellent** | ✅ **Excellent** |
| **Java Support** | ✅ **Native** | ✅ Native | ✅ Good |
| **Management** | ✅ **Zero** | ❌ High | ✅ Low |
| **Learning Curve** | ✅ **Minimal** | ❌ High | ✅ Low |

## 🏆 **Final Recommendation: Choose Heroku**

**Heroku is absolutely the best choice for you because:**

1. **✅ Solves Your Problem**: Excellent IP reputation fixes 403 errors
2. **✅ Simplest Setup**: Deploy in minutes, not hours
3. **✅ Lowest Cost**: $12/month vs $24-27/month alternatives
4. **✅ Native Java**: Perfect for your Spring Boot app
5. **✅ Zero Management**: Focus on code, not infrastructure

**Migration Timeline:**
- **Day 1**: Set up Heroku account and deploy
- **Day 2**: Test thoroughly and migrate database
- **Day 3**: Switch DNS and monitor
- **Day 4**: Delete Railway resources

**Expected Results:**
- ✅ The Odds API 403 errors will be resolved
- ✅ Professional hosting infrastructure
- ✅ Better application performance
- ✅ Reliable database with backups
- ✅ Clean, professional IP addresses
- ✅ **$12/month total cost**

---

**Heroku gives you everything you need at the lowest cost with the simplest setup. It's the perfect solution for your situation!**
