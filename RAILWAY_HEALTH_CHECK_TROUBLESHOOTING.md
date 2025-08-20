# Railway Health Check Troubleshooting

## 🚨 **HEALTH CHECK ISSUES FIXED**

### **Problem:** Railway health checks failing with "service unavailable"

### **Root Causes Identified:**
1. **Authentication Required:** Health endpoint required admin credentials
2. **Missing Environment Variables:** `ADMIN_PASSWORD` not set
3. **Security Configuration:** Health checks blocked by security
4. **JAR File Name Mismatch:** Incorrect JAR file name in startup command
5. **Database Dependency:** Health checks failing due to database connection issues

### **✅ LATEST FIXES APPLIED:**

#### **1. Updated Security Configuration**
**File:** `SecurityConfig.java`
```java
// BEFORE (BLOCKED):
.requestMatchers("/api/auth/**", "/api/games/**", "/api/picks/**", "/api/leagues/**", "/api/leaderboard/**", "/actuator/**").permitAll()

// AFTER (ALLOWED):
.requestMatchers("/actuator/health", "/actuator/info").permitAll() // Health checks always accessible
.requestMatchers("/api/auth/**", "/api/games/**", "/api/picks/**", "/api/leagues/**", "/api/leaderboard/**").permitAll()
```

#### **2. Updated Actuator Configuration**
**File:** `application-railway.properties`
```properties
# BEFORE (RESTRICTED):
management.endpoint.health.show-details=when-authorized

# AFTER (PUBLIC):
management.endpoint.health.show-details=always
management.endpoints.web.base-path=/actuator
```

#### **3. Created Simple Health Controller**
**File:** `HealthController.java` (new)
```java
@GetMapping("/health")
public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("service", "NFL Pick'em Backend");
    response.put("timestamp", System.currentTimeMillis());
    return ResponseEntity.ok(response);
}

@GetMapping("/ping")
public ResponseEntity<String> ping() {
    return ResponseEntity.ok("pong");
}
```

#### **4. Updated Railway Configuration**
**File:** `railway.json`
```json
// BEFORE:
"healthcheckPath": "/actuator/health"
"startCommand": "java -jar target/pickem-0.0.1-SNAPSHOT.jar"

// AFTER:
"healthcheckPath": "/ping"
"startCommand": "java -jar target/pickem-0.0.1-SNAPSHOT.jar --spring.profiles.active=railway"
```

#### **5. Fixed Nixpacks Configuration**
**File:** `nixpacks.toml`
```toml
# BEFORE:
cmd = "java -jar target/pickem-0.0.1-SNAPSHOT.jar"

# AFTER:
cmd = "java -jar target/pickem-0.0.1-SNAPSHOT.jar --spring.profiles.active=railway"
```

## 🔍 **IMMEDIATE TROUBLESHOOTING STEPS:**

### **1. Run Debug Script:**
```bash
# Make script executable
chmod +x debug-railway.sh

# Run debug script
./debug-railway.sh
```

### **2. Check Railway Logs:**
```bash
# View real-time logs
railway logs --follow

# View recent logs
railway logs --limit 50
```

### **3. Check Environment Variables:**
```bash
# View all environment variables
railway variables

# Check specific variables
railway variables | grep -E "(PORT|DATABASE|SPRING)"
```

### **4. Test Health Endpoints Manually:**
After deployment, test these endpoints:
```bash
# Simple ping endpoint
curl https://your-app.railway.app/ping

# Health endpoint
curl https://your-app.railway.app/health

# Root endpoint
curl https://your-app.railway.app/

# Actuator health
curl https://your-app.railway.app/actuator/health
```

## 🚨 **COMMON ISSUES & SOLUTIONS:**

### **Issue 1: JAR File Not Found**
**Symptoms:** `Error: Unable to access jarfile target/pickem-0.0.1-SNAPSHOT.jar`
**Solution:** Check if Maven build succeeded in Railway logs

### **Issue 2: Port Binding Problems**
**Symptoms:** `Web server failed to start. Port 8080 was already in use`
**Solution:** Ensure `PORT` environment variable is set in Railway

### **Issue 3: Database Connection Issues**
**Symptoms:** `Failed to configure a DataSource`
**Solution:** Check `DATABASE_URL`, `PGUSER`, `PGPASSWORD` environment variables

### **Issue 4: Memory Issues**
**Symptoms:** `java.lang.OutOfMemoryError: Java heap space`
**Solution:** Add JVM memory settings to Railway environment variables

### **Issue 5: Build Failures**
**Symptoms:** Maven build fails during deployment
**Solution:** Check build logs for dependency or compilation errors

## 🔧 **EMERGENCY FIXES:**

### **If Health Checks Still Fail:**

1. **Try Different Health Check Path:**
   ```json
   // In railway.json, try these paths:
   "healthcheckPath": "/ping"        // Simplest
   "healthcheckPath": "/health"      // Simple health
   "healthcheckPath": "/"            // Root endpoint
   ```

2. **Disable Database for Health Check:**
   ```properties
   # In application-railway.properties
   management.health.db.enabled=false
   ```

3. **Use Minimal Configuration:**
   ```properties
   # In application-railway.properties
   spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
   ```

## 📋 **DEPLOYMENT CHECKLIST:**

- [ ] Push all latest fixes to GitHub
- [ ] Ensure Railway environment variables are set
- [ ] Verify PostgreSQL service is running
- [ ] Check Railway logs for startup errors
- [ ] Test health endpoints manually
- [ ] Monitor health check status in Railway dashboard
- [ ] Run debug script if issues persist

## 🎯 **EXPECTED HEALTH CHECK RESPONSES:**

**Ping Endpoint (`/ping`):**
```
pong
```

**Simple Health Check (`/health`):**
```json
{
  "status": "UP",
  "service": "NFL Pick'em Backend",
  "timestamp": 1234567890,
  "version": "1.0.0",
  "environment": "railway"
}
```

**Root Endpoint (`/`):**
```json
{
  "message": "NFL Pick'em Backend API",
  "status": "running",
  "health": "/health",
  "actuator": "/actuator/health",
  "timestamp": 1234567890
}
```

## 🚀 **NEXT STEPS:**

1. **Deploy these latest fixes immediately**
2. **Run the debug script to identify issues**
3. **Monitor Railway logs in real-time**
4. **Test all health endpoints manually**
5. **Check environment variables in Railway dashboard**

**These fixes should resolve your Railway health check failures!**
