# Jenkins Quick Start Guide

## ⚡ Quick Setup (5 Minutes)

### 1. Add JWT Secret to Jenkins
```bash
# Generate a JWT secret
openssl rand -base64 64

# Copy the output, then in Jenkins:
# 1. Go to: Manage Jenkins → Manage Credentials → System → Global credentials
# 2. Click "Add Credentials"
# 3. Kind: Secret text
# 4. Secret: <paste your generated secret>
# 5. ID: JWT_SECRET
# 6. Click "OK"
```

### 2. Create Pipeline Job
```
1. Jenkins Dashboard → New Item
2. Name: buy-01-pipeline
3. Type: Multibranch Pipeline
4. Click OK

Branch Sources:
- Add source → Git
- Project Repository: /home/student/ZONE01/JAVA/buy-01
- Build Configuration: by Jenkinsfile
- Click Save
```

### 3. Run Your First Build
```
1. Click "Scan Multibranch Pipeline Now"
2. Select your branch (e.g., jenkins, dev, or main)
3. Click the build number to see progress
4. Monitor Console Output
```

## 🎯 What Happens During Build

### For `jenkins` or feature branches:
```
✅ Checkout code
✅ Build all 4 backend services (parallel)
✅ Build frontend
❌ Skip Docker images
❌ Skip deployment
```

### For `dev` branch:
```
✅ Checkout code
✅ Build all services
✅ Build Docker images
✅ Deploy automatically (no approval)
✅ Health checks
```

### For `main` branch:
```
✅ Checkout code
✅ Build all services
✅ Build Docker images
⏸️  Wait for manual approval (you need to click "Deploy")
✅ Deploy after approval
✅ Health checks
```

## 🔍 Monitoring Your Build

### View Progress
- **Blue Ocean UI**: Better visualization
  ```
  http://localhost:8080/blue/organizations/jenkins/buy-01-pipeline
  ```
- **Classic UI**: Traditional Jenkins view
  ```
  http://localhost:8080/job/buy-01-pipeline
  ```

### Check Deployed Application
After successful deployment:
```bash
# Check all services are running
docker-compose ps

# Access the application
Frontend: https://localhost:4443
API: http://localhost:8080

# View logs
docker-compose logs -f
```

## 🐛 Common Issues

### "JWT_SECRET credential not found"
```
Solution: Add the credential in Jenkins (see step 1 above)
```

### "Permission denied: jenkins-deploy.sh"
```bash
chmod +x jenkins-deploy.sh
```

### "Port already in use"
```bash
# Stop existing services
docker-compose down
./docker-stop.sh
```

### Build stuck at Maven/npm stage
```bash
# Check if you have enough memory
free -h

# Clean Docker to free space
docker system prune -a
```

## 📚 Full Documentation

For detailed setup, troubleshooting, and customization:
- **Complete Guide**: [docs/JENKINS_SETUP.md](./JENKINS_SETUP.md)
- **Architecture**: [README.md](../README.md)
- **Security**: [JWT_SECURITY.md](./JWT_SECURITY.md)

## 🎉 Success Indicators

When everything is working:
```
✅ Jenkins shows green build
✅ All Docker containers are "Up (healthy)"
✅ https://localhost:4443 loads the frontend
✅ No errors in docker-compose logs
```

## 💡 Pro Tips

1. **Fast Feedback**: Start with `jenkins` branch to test pipeline without deployment
2. **Save Resources**: Let `dev` auto-deploy for rapid iteration
3. **Production Safety**: Use `main` branch with manual approval
4. **Clean Builds**: Run `docker system prune` weekly to free space
5. **Monitor Logs**: Keep `docker-compose logs -f` running in a separate terminal

## 🚀 Next Build

After your first successful build:
```bash
# Make a code change
# Commit and push
git add .
git commit -m "test: trigger Jenkins build"
git push origin jenkins

# Jenkins will automatically detect and build
# Check Jenkins UI to see the new build
```

Good luck! 🎯

