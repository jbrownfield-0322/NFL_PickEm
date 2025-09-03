# Heroku + GitHub Integration Guide for NFL Pickem

## 🔗 **Complete GitHub + Heroku Setup**

This guide covers everything you need to connect your GitHub repo with Heroku for seamless deployment.

## 📋 **Prerequisites Checklist**

### **✅ Required Accounts**
- [ ] **GitHub Account** - [github.com](https://github.com)
- [ ] **Heroku Account** - [heroku.com](https://heroku.com)
- [ ] **Git installed** on your local machine

### **✅ Required Tools**
- [ ] **Heroku CLI** installed
- [ ] **Git** configured with your credentials
- [ ] **Maven** for building your Java app

## 🚀 **Step-by-Step Setup**

### **Step 1: Prepare Your Local Repository**

```bash
# Navigate to your project directory
cd C:\Users\jbrow\NFL_Pickem_repo

# Check git status
git status

# Add all new files
git add .

# Commit your changes
git commit -m "Add Heroku deployment configuration"

# Push to GitHub
git push origin main
```

### **Step 2: Create Heroku App**

```bash
# Login to Heroku
heroku login

# Create new app (replace [YOUR-NAME] with your preferred name)
heroku create nfl-pickem-[YOUR-NAME]

# Verify app was created
heroku apps
```

### **Step 3: Connect Heroku to GitHub**

```bash
# Add Heroku remote to your git repo
heroku git:remote -a nfl-pickem-[YOUR-NAME]

# Verify remote was added
git remote -v
```

### **Step 4: Set Up Database and Environment**

```bash
# Add PostgreSQL database
heroku addons:create heroku-postgresql:basic

# Set environment variables
heroku config:set THEODDS_API_KEY=04f5a0643a9fe4b5b3b390c8037ae885
heroku config:set SPRING_PROFILES_ACTIVE=heroku
heroku config:set ODDS_SCHEDULING_FREQUENCY=4
heroku config:set ODDS_GAME_DAY_FREQUENCY=1

# Verify configuration
heroku config
```

### **Step 5: Deploy Your App**

```bash
# Build your application
mvn clean package -DskipTests

# Deploy to Heroku
git push heroku main

# Open your app
heroku open
```

## 🔄 **GitHub Actions Auto-Deploy (Optional)**

### **Step 1: Get Heroku API Key**

```bash
# Generate API key
heroku authorizations:create

# Copy the Token value (you'll need this for GitHub)
```

### **Step 2: Set GitHub Secrets**

1. **Go to your GitHub repo** → **Settings** → **Secrets and variables** → **Actions**
2. **Add these secrets:**
   - `HEROKU_API_KEY` = Your Heroku API token
   - `HEROKU_APP_NAME` = Your Heroku app name (e.g., `nfl-pickem-jbrownfield`)
   - `HEROKU_EMAIL` = Your Heroku email

### **Step 3: Test Auto-Deploy**

```bash
# Make a small change to your code
# Commit and push to GitHub
git add .
git commit -m "Test auto-deploy"
git push origin main

# Check GitHub Actions tab to see deployment progress
```

## 🌐 **Post-Deployment Verification**

### **1. Test Your Application**

```bash
# Health check
curl https://nfl-pickem-[YOUR-NAME].herokuapp.com/actuator/health

# Test The Odds API
curl https://nfl-pickem-[YOUR-NAME].herokuapp.com/api/admin/odds/test-connectivity

# Check logs
heroku logs --tail
```

### **2. Verify Database Connection**

```bash
# Check database status
heroku pg:info

# Connect to database
heroku pg:psql

# Test connection
\dt
\q
```

## 🔧 **Troubleshooting Common Issues**

### **Issue 1: Build Fails**
```bash
# Check build logs
heroku logs --tail

# Common fixes:
# - Ensure Java 17 is specified in system.properties
# - Check Maven dependencies in pom.xml
# - Verify Procfile syntax
```

### **Issue 2: App Crashes on Startup**
```bash
# Check startup logs
heroku logs --tail

# Common fixes:
# - Verify environment variables are set
# - Check database connection
# - Ensure correct Spring profile
```

### **Issue 3: Database Connection Fails**
```bash
# Check database status
heroku pg:info

# Verify DATABASE_URL is set
heroku config:get DATABASE_URL

# Test database connection
heroku pg:psql
```

## 📊 **Monitoring and Management**

### **1. App Performance**
```bash
# Check dyno status
heroku ps

# Monitor logs
heroku logs --tail

# Check app metrics
heroku addons:open papertrail
```

### **2. Database Management**
```bash
# Database info
heroku pg:info

# Database logs
heroku pg:logs

# Database maintenance
heroku pg:maintenance
```

### **3. Scaling (Optional)**
```bash
# Scale to 1 dyno (recommended for production)
heroku ps:scale web=1

# Check current scaling
heroku ps
```

## 🔒 **Security Best Practices**

### **1. Environment Variables**
- ✅ **Never commit secrets** to git
- ✅ **Use Heroku config** for sensitive data
- ✅ **Rotate API keys** regularly

### **2. Database Security**
- ✅ **Automatic backups** with Heroku Postgres
- ✅ **SSL connections** by default
- ✅ **Access control** via Heroku dashboard

### **3. App Security**
- ✅ **HTTPS enforced** by default
- ✅ **Automatic SSL** certificates
- ✅ **DDoS protection** included

## 🔄 **Workflow After Setup**

### **Daily Development Workflow**
```bash
# 1. Make changes to your code
# 2. Test locally
mvn clean package -DskipTests
java -jar target/pickem-0.0.1-SNAPSHOT.jar

# 3. Commit and push to GitHub
git add .
git commit -m "Description of changes"
git push origin main

# 4. GitHub Actions automatically deploys to Heroku
# 5. Test on Heroku
heroku open
```

### **Manual Deployment (if needed)**
```bash
# Build and deploy manually
mvn clean package -DskipTests
git push heroku main

# Check deployment status
heroku ps
```

## 📞 **Support and Resources**

### **1. Heroku Documentation**
- [Getting Started with Java](https://devcenter.heroku.com/articles/getting-started-with-java)
- [Heroku Postgres](https://devcenter.heroku.com/articles/heroku-postgresql)
- [Deploying Spring Boot](https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku)

### **2. GitHub Actions**
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Heroku Deploy Action](https://github.com/akhileshns/heroku-deploy)

### **3. Community Support**
- [Heroku Community](https://help.heroku.com/)
- [GitHub Community](https://github.community/)
- [Stack Overflow](https://stackoverflow.com/)

## 🎯 **Expected Results**

### **After Setup, You'll Have:**
- ✅ **Automatic deployment** from GitHub to Heroku
- ✅ **Professional hosting** with excellent IP reputation
- ✅ **Managed database** with automatic backups
- ✅ **Zero infrastructure management**
- ✅ **Predictable pricing** at $12/month
- ✅ **The Odds API 403 errors resolved**

### **Your Workflow Will Be:**
1. **Code locally** → **Push to GitHub** → **Auto-deploy to Heroku**
2. **Test on Heroku** → **Verify The Odds API works**
3. **Monitor performance** → **Scale if needed**

---

**This setup gives you a professional, automated deployment pipeline that will solve your The Odds API access issues!**
