# Railway Deployment Guide for NFL Pick'em App

This guide will walk you through deploying your NFL Pick'em application on Railway with a PostgreSQL database.

## Prerequisites

- A Railway account (sign up at [railway.app](https://railway.app))
- Your code pushed to a GitHub repository
- Railway CLI (optional but recommended)

## Step 1: Set Up Railway Project

### Option A: Using Railway Dashboard

1. **Create a new project:**
   - Go to [railway.app](https://railway.app)
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Connect your GitHub account and select your repository

2. **Add PostgreSQL database:**
   - In your Railway project dashboard
   - Click "New Service"
   - Select "Database" → "PostgreSQL"
   - Railway will automatically create the database and provide connection details

### Option B: Using Railway CLI

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login to Railway
railway login

# Initialize project
railway init

# Add PostgreSQL database
railway add
# Select "Database" → "PostgreSQL"
```

## Step 2: Configure Environment Variables

In your Railway project dashboard, go to the "Variables" tab and add these environment variables:

### For Backend Service:
```env
SPRING_PROFILES_ACTIVE=railway
PORT=8080
```

### For Frontend Service:
```env
REACT_APP_API_URL=https://your-backend-service-url.railway.app
PORT=3000
```

**Note:** Railway automatically provides these database variables to your backend service:
- `DATABASE_URL` - Full PostgreSQL connection string
- `PGUSER` - Database username
- `PGPASSWORD` - Database password
- `PGHOST` - Database host
- `PGPORT` - Database port
- `PGDATABASE` - Database name

## Step 3: Deploy Backend Service

1. **Create backend service:**
   - In your Railway project, click "New Service"
   - Select "GitHub Repo"
   - Choose your repository
   - Set the service name to "backend"

2. **Configure build settings:**
   - Railway will automatically detect it's a Java/Maven project
   - The `nixpacks.toml` file will handle the build process
   - Set the root directory to `/` (root of your repo)

3. **Deploy:**
   - Railway will automatically build and deploy your backend
   - Monitor the build logs for any issues

## Step 4: Deploy Frontend Service

1. **Create frontend service:**
   - In your Railway project, click "New Service"
   - Select "GitHub Repo"
   - Choose the same repository
   - Set the service name to "frontend"

2. **Configure build settings:**
   - Set the root directory to `/frontend`
   - Railway will use the `frontend/nixpacks.toml` file

3. **Update API URL:**
   - In the frontend service variables, set:
   ```env
   REACT_APP_API_URL=https://your-backend-service-url.railway.app
   ```

4. **Deploy:**
   - Railway will build and deploy your frontend
   - The frontend will be served using the `serve` package

## Step 5: Configure Custom Domains (Optional)

1. **Add custom domain:**
   - Go to your service settings
   - Click "Domains"
   - Add your custom domain
   - Configure DNS records as instructed

2. **SSL certificates:**
   - Railway automatically provides SSL certificates
   - No additional configuration needed

## Step 6: Update Frontend API Configuration

Update your frontend to use the Railway backend URL. In your React components, update the API base URL:

```javascript
// In components like GameList.js, Login.js, etc.
const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080';
```

## Step 7: Test Your Deployment

1. **Check backend health:**
   - Visit: `https://your-backend-url.railway.app/actuator/health`
   - Should return a healthy status

2. **Test frontend:**
   - Visit your frontend URL
   - Test registration, login, and game functionality

3. **Check database:**
   - Verify data is being saved to PostgreSQL
   - Check Railway dashboard for database metrics

## Step 8: Monitor and Maintain

### Viewing Logs
```bash
# Using Railway CLI
railway logs

# Or view in Railway dashboard
# Go to your service → "Deployments" → Click on deployment → "Logs"
```

### Updating Your App
1. Push changes to your GitHub repository
2. Railway will automatically detect changes and redeploy
3. Monitor the deployment logs for any issues

### Database Management
- **Backups:** Railway automatically backs up your PostgreSQL database
- **Access:** Use Railway's database connection details to connect with any PostgreSQL client
- **Scaling:** Railway can automatically scale your database based on usage

## Troubleshooting

### Common Issues

1. **Build Failures:**
   - Check the build logs in Railway dashboard
   - Ensure all dependencies are properly specified
   - Verify Java 17 and Maven are available

2. **Database Connection Issues:**
   - Verify environment variables are set correctly
   - Check that the PostgreSQL service is running
   - Ensure the database URL format is correct

3. **Frontend API Errors:**
   - Verify `REACT_APP_API_URL` is set correctly
   - Check CORS configuration in backend
   - Ensure backend service is healthy

4. **Port Issues:**
   - Railway automatically assigns ports via `PORT` environment variable
   - Ensure your app uses `process.env.PORT` or `$PORT`

### Health Checks

Railway uses the `/actuator/health` endpoint to monitor your backend:
- If health checks fail, Railway will restart your service
- Monitor the health endpoint to ensure your app is running properly

### Performance Monitoring

- Use Railway's built-in metrics to monitor:
  - CPU and memory usage
  - Request latency
  - Error rates
  - Database performance

## Cost Optimization

1. **Free Tier:**
   - Railway offers a generous free tier
   - Monitor usage in the dashboard

2. **Scaling:**
   - Start with minimal resources
   - Scale up as needed based on usage

3. **Database:**
   - PostgreSQL on Railway is pay-as-you-use
   - Monitor database usage and optimize queries

## Security Best Practices

1. **Environment Variables:**
   - Never commit sensitive data to your repository
   - Use Railway's environment variable system
   - Rotate database passwords regularly

2. **CORS Configuration:**
   - Update CORS settings to only allow your frontend domain
   - Remove wildcard origins in production

3. **Database Security:**
   - Railway automatically secures your PostgreSQL instance
   - Use connection pooling for better performance

## Support

- **Railway Documentation:** [docs.railway.app](https://docs.railway.app)
- **Railway Discord:** [discord.gg/railway](https://discord.gg/railway)
- **GitHub Issues:** For application-specific issues

Your NFL Pick'em app should now be successfully deployed on Railway with a PostgreSQL database!
