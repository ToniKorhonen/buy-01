# Buy-01 E-Commerce Application

A full-stack e-commerce platform built with Spring Boot microservices backend and Angular frontend.

## 🚀 Quick Start

### Docker Deployment (Recommended)

The easiest way to run the entire application with all services:

```bash
./docker-start.sh
```

Access the application at:
- **Frontend (HTTPS)**: https://localhost:4443
- **Frontend (HTTP)**: http://localhost:4200 (redirects to HTTPS)
- **API Gateway**: http://localhost:8080

To stop all services:
```bash
./docker-stop.sh
```

### Local Development Mode

For development with hot-reload and debugging:

1. **Start MongoDB**:
   ```bash
   ./start-dev-db.sh
   ```

2. **Start Backend Services** (in separate terminals):
   ```bash
   # Terminal 1 - User Service
   cd Backend/user-service
   ./mvnw spring-boot:run
   
   # Terminal 2 - Product Service
   cd Backend/product-service
   ./mvnw spring-boot:run
   
   # Terminal 3 - Media Service
   cd Backend/media-service
   ./mvnw spring-boot:run
   
   # Terminal 4 - API Gateway
   cd Backend/api-gateway
   ./mvnw spring-boot:run
   ```

3. **Start Frontend**:
   ```bash
   cd Frontend
   npm install  # First time only
   npm start
   ```

Access at: https://localhost:4443

## 🔄 CI/CD with Jenkins

This project includes a complete Jenkins pipeline for automated building, testing, and deployment.

### Quick Jenkins Setup

1. **Add JWT Secret to Jenkins**:
   ```bash
   # Generate secret
   openssl rand -base64 64
   
   # Add to Jenkins: Manage Jenkins → Credentials → Add Secret Text
   # ID: JWT_SECRET
   ```

2. **Create Pipeline Job**:
   - New Item → Multibranch Pipeline
   - Repository: `/home/student/ZONE01/JAVA/buy-01`
   - Build Configuration: by Jenkinsfile

3. **Branch Strategy**:
   - `main` → Build + Manual Approval + Deploy
   - `dev` → Build + Auto-Deploy
   - `jenkins` / feature branches → Build Only

### Pipeline Features

- ✅ Parallel builds for all microservices
- ✅ Docker image creation and tagging
- ✅ Automated health checks
- ✅ Auto-rollback on deployment failure
- ✅ Branch-aware deployment strategy

📚 **Full Documentation**: 
- [Quick Start Guide](docs/JENKINS_QUICK_START.md)
- [Complete Setup](docs/JENKINS_SETUP.md)

## 🔍 Code Quality with SonarCloud

Automated code quality analysis runs on every push using GitHub Actions.

### Quick Setup (5 minutes)

1. **Get SonarCloud Token**: https://sonarcloud.io → My Account → Security → Generate Token
2. **Add GitHub Secrets**:
   - `SONAR_TOKEN` - Your SonarCloud token
   - `SONAR_ORGANIZATION` - Your SonarCloud organization key
3. **Push to GitHub** - Analysis runs automatically!

### What's Analyzed

- ✅ All 4 backend microservices (user, product, media, gateway)
- ✅ Frontend Angular application
- ✅ Code coverage, bugs, vulnerabilities, code smells
- ✅ Technical debt and maintainability metrics

### View Results

- **GitHub**: Actions tab → SonarCloud Analysis workflow
- **SonarCloud**: https://sonarcloud.io → Your organization

📚 **Documentation**: 
- [Quick Start Guide](docs/GITHUB_ACTIONS_QUICK_START.md)
- [Complete Guide](docs/GITHUB_ACTIONS_SONARCLOUD.md)
- [Troubleshooting Common Issues](docs/SONARCLOUD_TROUBLESHOOTING.md)
- [Quick Fix: Automatic Analysis Error](docs/SONARCLOUD_QUICK_FIX.md)

### Jenkins vs GitHub Actions

Both work together for comprehensive CI/CD:

- **Jenkins**: Local builds, deployment, full control
- **GitHub Actions**: Cloud-based analysis, PR checks, SonarCloud integration

## 📋 Prerequisites

### For Docker Deployment
- Docker
- Docker Compose

### For Local Development
- Java 17 or higher
- Maven
- Node.js 20+
- MongoDB

## 🏗️ Architecture

### Microservices
- **API Gateway** (Port 8080) - Routes requests to backend services
- **User Service** (Port 8081) - Authentication and user management
- **Product Service** (Port 8082) - Product catalog
- **Media Service** (Port 8083) - Image upload and storage

### Frontend
- **Angular 20** with Express.js server
- **HTTPS** support with security headers
- **HTTP to HTTPS** redirect on port 4200 → 4443

### Database
- **MongoDB** - NoSQL database for all services

## 🔒 Security Features

- JWT-based authentication
- Content Security Policy (CSP)
- HTTPS/TLS encryption
- Security headers (X-Frame-Options, X-Content-Type-Options, etc.)
- Protection against common web vulnerabilities

## 📝 Environment Variables

The application uses a `.env` file for configuration. Key variables:

```bash
# JWT Configuration
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=3600000

# MongoDB
# Note: Docker deployment uses port 27018 to avoid conflicts with local MongoDB (27017)
MONGODB_HOST=localhost
MONGODB_PORT=27018  # For Docker deployment (local MongoDB uses 27017 for tests)
```

## 🐳 Docker Commands

```bash
# Start all services
./docker-start.sh

# Stop all services
./docker-stop.sh

# View logs
docker compose logs -f [service-name]

# Restart a service
docker compose restart [service-name]

# Rebuild and restart
docker compose up --build -d

# Clean up everything
docker compose down -v
```

## 🛠️ Development Commands

```bash
# Backend - Build
cd Backend/[service-name]
./mvnw clean package

# Backend - Run tests
./mvnw test

# Frontend - Install dependencies
cd Frontend
npm install

# Frontend - Development server
npm start

# Frontend - Build for production
npm run build
```

## 📦 Project Structure

```
buy-01/
├── Backend/
│   ├── api-gateway/       # API Gateway service
│   ├── user-service/      # User management
│   ├── product-service/   # Product catalog
│   └── media-service/     # Media uploads
├── Frontend/              # Angular application
├── docker-compose.yml     # Docker orchestration
├── .env                   # Environment variables
└── docs/                  # Documentation
```

## 🔧 Troubleshooting

### Port Already in Use

If you get "address already in use" errors:

**For Docker:**
```bash
# Stop local MongoDB
sudo systemctl stop mongod
# Or use the docker-start.sh script (handles this automatically)
```

**For Development:**
```bash
# Stop Docker MongoDB
docker compose down
```

### MongoDB Connection Issues

**Development mode:** Ensure MongoDB is running
```bash
./start-dev-db.sh
```

**Docker mode:** MongoDB starts automatically with docker-compose

### Frontend Not Accessible

1. Check if all services are healthy:
   ```bash
   docker compose ps
   ```

2. Check frontend logs:
   ```bash
   docker logs buy01-frontend
   ```

## 📚 Additional Documentation

- [Docker Deployment Guide](DOCKER_DEPLOYMENT.md)
- [Docker Quick Start](DOCKER_QUICK_START.md)
- [JWT Security](docs/JWT_SECURITY.md)
- [SSL Certificate Guide](Frontend/docs/SSL_CERTIFICATE_GUIDE.md)

## 🤝 Contributing

1. Ensure MongoDB is running
2. Run all services locally before committing
3. Test with both Docker and local development setups
4. Follow security best practices

## 📄 License

This project is for educational purposes.
# Test polling
