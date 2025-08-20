# NFL Pick'em App Deployment Guide

This guide covers multiple deployment options for the NFL Pick'em application.

## Prerequisites

- Docker and Docker Compose installed
- Java 17+ (for local development)
- Node.js 18+ (for local development)
- PostgreSQL (for production database)

## Option 1: Docker Compose (Recommended for Production)

### Quick Start

1. **Clone the repository and navigate to the project directory:**
   ```bash
   cd nfl-pickem
   ```

2. **Build and start all services:**
   ```bash
   docker-compose up --build
   ```

3. **Access the application:**
   - Frontend: http://localhost
   - Backend API: http://localhost:8080
   - Database: localhost:5432

### Environment Variables

Create a `.env` file in the root directory to customize the deployment:

```env
# Database Configuration
POSTGRES_DB=nflpickem
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_secure_password

# Spring Boot Configuration
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/nflpickem
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_secure_password

# Application Configuration
SERVER_PORT=8080
```

### Production Considerations

1. **Database Persistence:**
   - Data is automatically persisted in a Docker volume
   - Backup the volume: `docker volume ls` then `docker run --rm -v nfl-pickem_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/db-backup.tar.gz -C /data .`

2. **SSL/HTTPS:**
   - Add a reverse proxy (nginx/traefik) with SSL certificates
   - Update the nginx configuration to handle HTTPS

3. **Monitoring:**
   - Add health checks to the docker-compose.yml
   - Set up logging aggregation

## Option 2: Cloud Deployment

### AWS Deployment

1. **EC2 Instance:**
   ```bash
   # Install Docker on EC2
   sudo yum update -y
   sudo yum install -y docker
   sudo service docker start
   sudo usermod -a -G docker ec2-user
   
   # Install Docker Compose
   sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   sudo chmod +x /usr/local/bin/docker-compose
   
   # Clone and deploy
   git clone <your-repo>
   cd nfl-pickem
   docker-compose up -d
   ```

2. **RDS Database:**
   - Create an RDS PostgreSQL instance
   - Update the `SPRING_DATASOURCE_URL` in docker-compose.yml
   - Use AWS Secrets Manager for database credentials

3. **Load Balancer:**
   - Set up Application Load Balancer
   - Configure SSL certificates
   - Set up auto-scaling groups

### Heroku Deployment

1. **Create Heroku app:**
   ```bash
   heroku create your-nfl-pickem-app
   ```

2. **Add PostgreSQL addon:**
   ```bash
   heroku addons:create heroku-postgresql:hobby-dev
   ```

3. **Deploy backend:**
   ```bash
   heroku config:set SPRING_PROFILES_ACTIVE=prod
   git push heroku main
   ```

4. **Deploy frontend:**
   - Build the React app: `npm run build`
   - Serve static files from the backend or use a separate hosting service

### Google Cloud Platform

1. **Cloud Run (Backend):**
   ```bash
   # Build and push container
   gcloud builds submit --tag gcr.io/PROJECT_ID/nfl-pickem-backend
   
   # Deploy to Cloud Run
   gcloud run deploy nfl-pickem-backend \
     --image gcr.io/PROJECT_ID/nfl-pickem-backend \
     --platform managed \
     --region us-central1 \
     --allow-unauthenticated
   ```

2. **Cloud SQL (Database):**
   - Create PostgreSQL instance
   - Update connection strings

3. **Firebase Hosting (Frontend):**
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase init hosting
   npm run build
   firebase deploy
   ```

## Option 3: Traditional Server Deployment

### Backend Deployment

1. **Build the JAR:**
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. **Set up PostgreSQL:**
   ```bash
   sudo apt-get update
   sudo apt-get install postgresql postgresql-contrib
   sudo -u postgres createdb nflpickem
   sudo -u postgres createuser pickem_user
   sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE nflpickem TO pickem_user;"
   ```

3. **Run the application:**
   ```bash
   java -jar target/pickem-0.0.1-SNAPSHOT.jar \
     --spring.profiles.active=prod \
     --spring.datasource.url=jdbc:postgresql://localhost:5432/nflpickem \
     --spring.datasource.username=pickem_user \
     --spring.datasource.password=your_password
   ```

### Frontend Deployment

1. **Build the React app:**
   ```bash
   cd frontend
   npm install
   npm run build
   ```

2. **Serve with nginx:**
   ```bash
   sudo apt-get install nginx
   sudo cp -r build/* /var/www/html/
   sudo systemctl restart nginx
   ```

## Security Considerations

1. **Environment Variables:**
   - Never commit sensitive data to version control
   - Use environment variables for all secrets
   - Consider using a secrets management service

2. **Database Security:**
   - Use strong passwords
   - Restrict database access to application servers only
   - Enable SSL connections

3. **Application Security:**
   - Enable HTTPS in production
   - Set up proper CORS configuration
   - Implement rate limiting
   - Regular security updates

4. **Monitoring and Logging:**
   - Set up application monitoring (e.g., New Relic, DataDog)
   - Configure centralized logging
   - Set up alerts for errors and performance issues

## Maintenance

1. **Database Backups:**
   ```bash
   # PostgreSQL backup
   pg_dump -h localhost -U postgres nflpickem > backup.sql
   
   # Restore
   psql -h localhost -U postgres nflpickem < backup.sql
   ```

2. **Application Updates:**
   ```bash
   # Pull latest changes
   git pull origin main
   
   # Rebuild and restart
   docker-compose down
   docker-compose up --build -d
   ```

3. **Health Checks:**
   - Monitor application endpoints
   - Set up automated health checks
   - Configure auto-restart policies

## Troubleshooting

### Common Issues

1. **Database Connection Errors:**
   - Check database credentials
   - Verify network connectivity
   - Ensure database is running

2. **CORS Errors:**
   - Update CORS configuration in SecurityConfig
   - Check frontend API base URL

3. **Memory Issues:**
   - Increase JVM heap size: `-Xmx2g -Xms1g`
   - Monitor memory usage

4. **Port Conflicts:**
   - Check if ports 80, 8080, 5432 are available
   - Update docker-compose.yml if needed

### Logs

```bash
# View application logs
docker-compose logs -f backend

# View database logs
docker-compose logs -f db

# View frontend logs
docker-compose logs -f frontend
```

## Support

For deployment issues or questions, please refer to:
- Spring Boot documentation
- Docker documentation
- React deployment guide
- Your cloud provider's documentation
