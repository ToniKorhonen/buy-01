# Jenkins CI/CD Setup Guide

This guide will help you set up Jenkins for the Buy-01 e-commerce application.

## 📋 Prerequisites

- Jenkins installed and running locally
- Docker and Docker Compose installed
- Git configured
- Java 17+ and Maven (if running Jenkins locally without Docker)
- Node.js 20+ (if running Jenkins locally without Docker)

## 🔧 Jenkins Configuration

### 1. Install Required Jenkins Plugins

Navigate to **Manage Jenkins** → **Manage Plugins** → **Available** and install:

- **Docker Pipeline** - For Docker commands in pipeline
- **Git Plugin** - For Git repository integration
- **Pipeline** - Core pipeline functionality
- **Credentials Binding Plugin** - For secure credential handling
- **Workspace Cleanup Plugin** - For cleaning workspace

### 2. Configure Jenkins Credentials

Navigate to **Manage Jenkins** → **Manage Credentials** → **System** → **Global credentials**

#### Add JWT Secret
1. Click **Add Credentials**
2. Kind: **Secret text**
3. Secret: Your JWT secret (the one from `.env` file)
4. ID: `JWT_SECRET`
5. Description: `JWT Secret for Buy-01 Application`

**To generate a new JWT secret:**
```bash
openssl rand -base64 64
```

### 3. Create Jenkins Pipeline Job

1. Go to Jenkins Dashboard → **New Item**
2. Enter name: `buy-01-pipeline` (or your preferred name)
3. Select **Multibranch Pipeline**
4. Click **OK**

#### Configure Branch Sources
1. **Branch Sources** → **Add source** → **Git**
2. **Project Repository**: `/home/student/ZONE01/JAVA/buy-01` (or your git remote URL)
3. **Credentials**: Add if using remote repository
4. **Behaviors**: 
   - Discover branches
   - Discover tags
5. **Build Configuration**:
   - Mode: **by Jenkinsfile**
   - Script Path: `Jenkinsfile`

#### Configure Build Triggers
- **Scan Multibranch Pipeline Triggers**: 
  - ✅ Periodically if not otherwise run
  - Interval: `1 hour` (or as preferred)

### 4. Save and Run

1. Click **Save**
2. Jenkins will automatically scan for branches
3. It will detect `main`, `dev`, and `jenkins` branches
4. Click **Scan Multibranch Pipeline Now** if needed

## 🌳 Branch Strategy

The pipeline behaves differently based on the branch:

| Branch | Build | Test | Deploy | Approval Required |
|--------|-------|------|--------|-------------------|
| `main` | ✅ | ✅ | ✅ | ✅ Manual approval |
| `dev` | ✅ | ✅ | ✅ | ❌ Auto-deploy |
| `jenkins` | ✅ | ✅ | ❌ | N/A |
| Feature branches | ✅ | ✅ | ❌ | N/A |

## 🚀 Pipeline Stages

### 1. **Checkout**
- Checks out code from Git
- Displays build information

### 2. **Environment Setup**
- Creates `.env` file from Jenkins credentials
- Validates environment variables

### 3. **Build Services** (Parallel)
- User Service (Maven)
- Product Service (Maven)
- Media Service (Maven)
- API Gateway (Maven)
- Frontend (npm)

### 4. **Build Docker Images**
- Only runs for `main` and `dev` branches
- Builds all services with docker-compose
- Tags images with build number and git commit

### 5. **Deployment Approval** (main branch only)
- Manual approval gate
- 30-minute timeout
- Only admins can approve

### 6. **Deploy**
- Runs `jenkins-deploy.sh`
- Performs health checks
- Auto-rollback on failure

## 📊 Monitoring the Pipeline

### View Build Status
1. Go to your pipeline job
2. Select the branch
3. Click on the build number
4. View **Console Output**

### Access Deployed Application
After successful deployment:
- **Frontend (HTTPS)**: https://localhost:4443
- **Frontend (HTTP)**: http://localhost:4200
- **API Gateway**: http://localhost:8080

### Check Service Health
```bash
# View running containers
docker-compose ps

# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f user-service
```

## 🔒 Security Best Practices

1. **Never commit `.env` file** - It's in `.gitignore`
2. **Store secrets in Jenkins Credentials** - Use credentials binding
3. **Rotate JWT secrets periodically** - Update Jenkins credential
4. **Review deployment approvals** - Check build logs before approving
5. **Limit deployment permissions** - Configure submitter list in Jenkinsfile

## 🐛 Troubleshooting

### Build Fails at Maven Stage
```bash
# Check Java version (must be 17+)
java -version

# Check Maven
./mvnw --version

# Clean build locally
cd Backend/user-service
./mvnw clean install
```

### Build Fails at npm Stage
```bash
# Check Node version (must be 20+)
node --version

# Clean install
cd Frontend
rm -rf node_modules package-lock.json
npm install
```

### Docker Build Fails
```bash
# Check Docker is running
docker ps

# Clean Docker cache
docker system prune -a

# Rebuild manually
docker-compose build --no-cache
```

### Deployment Health Checks Fail
```bash
# Check if ports are already in use
netstat -tulpn | grep -E ':(8080|8081|8082|8083|4200|4443|27017)'

# Stop conflicting services
docker-compose down
./docker-stop.sh

# Check container logs
docker-compose logs
```

### Permission Issues with Scripts
```bash
# Make scripts executable
chmod +x jenkins-deploy.sh
chmod +x docker-start.sh
chmod +x docker-stop.sh
```

## 🔄 Rollback Procedure

If a deployment fails or causes issues:

1. **Automatic Rollback**: The pipeline automatically rolls back if health checks fail
2. **Manual Rollback**:
   ```bash
   # Stop current deployment
   docker-compose down
   
   # Deploy previous successful build
   # Find previous build number in Jenkins
   # Re-run that build
   ```

## 📝 Customization

### Modify Deployment Branches
Edit `Jenkinsfile` environment section:
```groovy
SHOULD_DEPLOY = """${sh(
    returnStatus: true,
    script: '''
        if [ "${BRANCH_NAME}" = "main" ] || [ "${BRANCH_NAME}" = "dev" ] || [ "${BRANCH_NAME}" = "staging" ]; then
            exit 0
        else
            exit 1
        fi
    '''
) == 0 ? 'true' : 'false'}"""
```

### Add Email Notifications
Add to `Jenkinsfile` post section:
```groovy
post {
    success {
        emailext (
            subject: "✅ Build Successful: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
            body: "Build completed successfully!",
            to: "your-email@example.com"
        )
    }
}
```

### Adjust Health Check Timeout
Edit `jenkins-deploy.sh`:
```bash
MAX_WAIT=600  # 10 minutes
```

## 📚 Additional Resources

- [Jenkins Pipeline Documentation](https://www.jenkins.io/doc/book/pipeline/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [Angular Testing](https://angular.io/guide/testing)

## 🎯 Next Steps

1. **Add Integration Tests**: Implement API tests in the pipeline
2. **Code Coverage Reports**: Add JaCoCo for Java, Istanbul for Angular
3. **Static Code Analysis**: Add SonarQube integration
4. **Container Registry**: Push images to Docker Hub or private registry
5. **Multiple Environments**: Add staging environment
6. **Slack Notifications**: Add Slack webhook for build notifications

