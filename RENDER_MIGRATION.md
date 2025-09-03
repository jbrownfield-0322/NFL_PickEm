# Migration Guide: Railway → Render

## 🚀 **What's Been Changed**

### **Files Removed:**
- `railway.json` - Railway deployment config
- `nixpacks.toml` - Railway build config  
- `railway-functions/` - Railway serverless functions

### **Files Added:**
- `render.yaml` - Render deployment config
- `src/main/resources/application-render.properties` - Render environment config

## 🔧 **Migration Steps**

### **Step 1: Set Up Render Account**
1. Go to [render.com](https://render.com)
2. Sign up with your GitHub account
3. Create a new account

### **Step 2: Create Postgres Database**
1. In Render dashboard, click **"New +"**
2. Select **"PostgreSQL"**
3. Choose **"Free"** plan (for testing)
4. Name it: `nfl-pickem-db`
5. **Save the database URL** - you'll need this!

### **Step 3: Create Web Service**
1. Click **"New +"** again
2. Select **"Web Service"**
3. Connect your **GitHub repository**
4. Name: `nfl-pickem`
5. Environment: **Java**
6. Build Command: `./mvnw clean package -DskipTests`
7. Start Command: `java -jar target/pickem-0.0.1-SNAPSHOT.jar`

### **Step 4: Configure Environment Variables**
In your web service settings, add these environment variables:

```
DATABASE_URL=postgresql://... (from your Postgres service)
THEODDS_API_KEY=04f5a0643a9fe4b5b3b390c8037ae885
SPRING_PROFILES_ACTIVE=render
PORT=8080
```

### **Step 5: Deploy**
1. **Commit and push** your changes to GitHub
2. Render will **auto-deploy** your application
3. Monitor the **deployment logs**
4. Test your endpoints

## 🗄️ **Database Migration**

### **Export from Railway:**
1. Go to your Railway project
2. Navigate to **Postgres** service
3. Click **"Connect"** → **"Connect with psql"**
4. Run: `pg_dump -h [host] -U [user] -d [database] > backup.sql`

### **Import to Render:**
1. Download your backup file
2. In Render Postgres, click **"Connect"**
3. Use the connection string to import:
   ```bash
   psql [render-db-url] < backup.sql
   ```

## 🌐 **URL Changes**

### **Old Railway URL:**
- `https://nflpickem.up.railway.app`

### **New Render URL:**
- `https://nfl-pickem.onrender.com` (or similar)

### **Update Frontend:**
If you hardcoded Railway URLs in your frontend, update them to the new Render URL.

## ✅ **Testing After Migration**

### **1. Health Check:**
```
https://your-app.onrender.com/actuator/health
```

### **2. Admin Endpoints:**
```
https://your-app.onrender.com/api/admin/odds/test-connectivity
https://your-app.onrender.com/api/admin/odds/test-connection
```

### **3. Main App:**
```
https://your-app.onrender.com/api/games
```

## 🚨 **Common Issues**

### **Build Failures:**
- Check **Maven version** compatibility
- Verify **Java 17** is available
- Check **build logs** for errors

### **Database Connection:**
- Verify **DATABASE_URL** format
- Check **Postgres service** is running
- Verify **environment variables** are set

### **Port Issues:**
- Ensure **PORT** environment variable is set
- Check **server.port** in application properties

## 💡 **Why This Should Work Better**

### **1. Better IP Reputation:**
- Render has **cleaner IP addresses**
- **Less likely** to be blocked by APIs
- **Better established** platform

### **2. Same Technology:**
- **Java 17** support
- **PostgreSQL** support
- **Git-based deployment**

### **3. Free Tier:**
- **Free Postgres** database
- **Free web service** (with limitations)
- **No monthly costs** for testing

## 🔄 **Rollback Plan**

If Render doesn't work:
1. **Keep your Railway project** running
2. **Don't delete** Railway resources yet
3. **Test Render** thoroughly first
4. **Migrate back** if needed

## 📞 **Support**

- **Render Support**: [docs.render.com](https://docs.render.com)
- **Migration Issues**: Check deployment logs
- **Database Issues**: Verify connection strings

---

**Good luck with the migration! Render should give you much better API access.**
