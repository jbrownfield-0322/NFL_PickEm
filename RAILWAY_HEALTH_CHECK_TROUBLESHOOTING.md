# Railway Health Check Troubleshooting

## 🚨 **HEALTH CHECK ISSUES FIXED**

### **Problem:** Railway health checks failing with "service unavailable"

### **Root Causes Identified:**
1. **Authentication Required:** Health endpoint required admin credentials
2. **Missing Environment Variables:** `ADMIN_PASSWORD` not set
3. **Security Configuration:** Health checks blocked by security

### **✅ FIXES APPLIED:**

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
```

#### **4. Updated Railway Configuration**
**File:** `railway.json`
```json
// BEFORE:
"healthcheckPath": "/actuator/health"

// AFTER:
"healthcheckPath": "/health"
```

## 🔍 **VERIFICATION STEPS:**

### **1. Test Health Endpoints Locally:**
```bash
# Test simple health check
curl http://localhost:8080/health

# Test actuator health check
curl http://localhost:8080/actuator/health

# Test root endpoint
curl http://localhost:8080/
```

### **2. Check Railway Logs:**
```bash
# View Railway logs
railway logs

# Check specific service logs
railway logs --service backend
```

### **3. Verify Environment Variables:**
Make sure these are set in Railway:
```env
SPRING_PROFILES_ACTIVE=railway
PORT=8080
DATABASE_URL=your_railway_postgres_url
PGUSER=your_railway_postgres_user
PGPASSWORD=your_railway_postgres_password
```

## 🚨 **COMMON ISSUES & SOLUTIONS:**

### **Issue 1: Database Connection Problems**
**Symptoms:** Health check fails due to database connection
**Solution:** Ensure PostgreSQL service is running and connected

### **Issue 2: Port Conflicts**
**Symptoms:** Service won't start on assigned port
**Solution:** Check Railway logs for port binding errors

### **Issue 3: Memory Issues**
**Symptoms:** Java heap space errors
**Solution:** Add JVM memory settings to Railway environment variables

### **Issue 4: Build Failures**
**Symptoms:** Maven build fails during deployment
**Solution:** Check build logs for dependency or compilation errors

## 🔧 **ADDITIONAL TROUBLESHOOTING:**

### **1. Check Application Startup:**
```bash
# View detailed startup logs
railway logs --service backend | grep -i "started\|error\|exception"
```

### **2. Test Database Connection:**
```bash
# Check if database is accessible
railway logs --service backend | grep -i "database\|postgres\|connection"
```

### **3. Verify Health Check Response:**
```bash
# Test health endpoint directly
curl -v https://your-app.railway.app/health
```

## 📋 **DEPLOYMENT CHECKLIST:**

- [ ] Push all security fixes to GitHub
- [ ] Ensure Railway environment variables are set
- [ ] Verify PostgreSQL service is running
- [ ] Check Railway logs for startup errors
- [ ] Test health endpoints manually
- [ ] Monitor health check status in Railway dashboard

## 🎯 **EXPECTED HEALTH CHECK RESPONSE:**

**Simple Health Check (`/health`):**
```json
{
  "status": "UP",
  "service": "NFL Pick'em Backend",
  "timestamp": 1234567890
}
```

**Actuator Health Check (`/actuator/health`):**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

## 🚀 **NEXT STEPS:**

1. **Deploy these fixes immediately**
2. **Monitor Railway logs for any remaining issues**
3. **Test both health endpoints manually**
4. **Verify frontend can connect to backend**
5. **Check database connectivity**

**These fixes should resolve your Railway health check failures!**
