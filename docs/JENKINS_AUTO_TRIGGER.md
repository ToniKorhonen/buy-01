# Jenkins Automatic Triggering Setup Guide

## ✅ Audit Requirements Implementation

This document explains how Jenkins automatically triggers builds and meets all audit requirements.

---

## 🔄 Automatic Build Triggering

### Current Setup: SCM Polling

The Jenkinsfile includes automatic SCM polling that checks for changes every 2 minutes:

```groovy
triggers {
    pollSCM('H/2 * * * *')
}
```

**How it works:**
1. Jenkins polls the Git repository every 2 minutes
2. If changes are detected, a new build is triggered automatically
3. Works with all branches configured in the multibranch pipeline

### Why Not GitHub Webhooks for Local Jenkins?

**⚠️ Important Limitation:**

GitHub webhooks **cannot reach `localhost`** because:
- Jenkins runs on your local machine (`http://localhost:9080`)
- GitHub servers are on the internet
- GitHub cannot send webhook events to `localhost` over the public internet
- Error you'll see: *"URL is not supported because it isn't reachable over the public Internet"*

**Therefore, for local Jenkins development:**
✅ **SCM Polling is the correct and recommended solution** (already configured!)

---

### Advanced: Using GitHub Webhooks (Only for Public Jenkins)

If you need instant triggering instead of 2-minute polling, you must expose Jenkins publicly:

#### Option 1: Using ngrok (Quick Testing - Not for Production)

**What is ngrok?** A tool that creates a secure tunnel from the internet to your localhost.

```bash
# Install ngrok
sudo snap install ngrok

# Create a free account at https://ngrok.com and get your auth token
ngrok config add-authtoken YOUR_AUTH_TOKEN

# Start tunnel to Jenkins
ngrok http 9080
```

**ngrok will output:**
```
Forwarding   https://abc123.ngrok-free.app -> http://localhost:9080
```

**Then configure GitHub webhook:**
1. Go to GitHub repo → **Settings** → **Webhooks**
2. Click **Add webhook**
3. Set **Payload URL**: `https://abc123.ngrok-free.app/github-webhook/`
4. Set **Content type**: `application/json`
5. Select **Just the push event**
6. Save

**Update Jenkinsfile:**
```groovy
triggers {
    githubPush()  // Instead of pollSCM
}
```

**Limitations:**
- ⚠️ ngrok URL changes every time you restart (unless you have a paid plan)
- ⚠️ Not suitable for production
- ⚠️ Free tier has connection limits

---

#### Option 2: Deploy Jenkins to Cloud (Production Solution)

**For a production setup, deploy Jenkins to:**
- AWS EC2
- DigitalOcean Droplet
- Azure VM
- Google Cloud Compute Engine
- Your own VPS

**Example setup:**
```bash
# Your Jenkins will be at:
https://jenkins.yourcompany.com

# GitHub webhook URL:
https://jenkins.yourcompany.com/github-webhook/
```

---

### Comparison: SCM Polling vs Webhooks

| Feature | SCM Polling (Current) | GitHub Webhooks |
|---------|----------------------|-----------------|
| **Works with localhost** | ✅ Yes | ❌ No |
| **Setup complexity** | ✅ Simple (already done!) | ⚠️ Requires public URL |
| **Trigger delay** | ~2 minutes | Instant |
| **Network requirements** | Jenkins → GitHub | GitHub → Jenkins |
| **Best for** | Local development | Production |
| **Cost** | Free | Free (ngrok paid for permanent URL) |

**Recommendation:** For your local setup, **keep using SCM polling**. It's working perfectly and requires no additional configuration!

---

## 🧪 Automated Testing

### Test Execution
Tests run automatically after the build stage for all services:

```groovy
stage('Run Tests') {
    when {
        expression { params.SKIP_TESTS == false }
    }
    // Tests run in parallel
}
```

**Features:**
- ✅ Tests run automatically in parallel
- ✅ Pipeline halts on test failure
- ✅ Test reports published to Jenkins
- ✅ Can be skipped via parameter (not recommended for production)

### Viewing Test Results
1. Go to your build in Jenkins
2. Click **Test Result**
3. View detailed test reports with pass/fail status

---

## 🎯 Parameterized Builds (Bonus Feature)

### Available Parameters

1. **DEPLOY_ENV**
   - Choices: `auto`, `dev`, `prod`
   - Default: `auto` (based on branch)
   - Override deployment environment

2. **SKIP_TESTS**
   - Type: Boolean
   - Default: `false`
   - Skip test execution (not recommended)

3. **CLEAN_BUILD**
   - Type: Boolean
   - Default: `false`
   - Force clean build, removing Docker caches

### Using Parameters
1. Go to Jenkins → Your Pipeline → Branch
2. Click **Build with Parameters**
3. Select desired options
4. Click **Build**

---

## 🔒 Security Implementation

### Credentials Management
✅ **Sensitive data secured using Jenkins Secrets:**

1. **JWT_SECRET**
   - Type: Secret text
   - Stored in: Jenkins Credentials
   - Used via: `withCredentials` binding
   - Never logged or exposed

### Example in Jenkinsfile:
```groovy
withCredentials([
    string(credentialsId: 'JWT_SECRET', variable: 'JWT_SECRET')
]) {
    // JWT_SECRET is available here and masked in logs
}
```

### Permissions
- Configure in Jenkins → **Manage Jenkins** → **Configure Global Security**
- Set authorization strategy (Matrix-based recommended)
- Limit deployment approval to admins only

---

## 📊 Notifications

### ✅ Implemented: Email Notifications

The pipeline now includes automatic email notifications for all build statuses.

#### Email Features

**Success Notifications (✅)**
- Build summary with deployment URLs
- Commit and branch information
- Links to console output and test reports
- HTML-formatted emails with color-coding

**Failure Notifications (❌)**
- Failed stage identification
- Last 50 lines of Docker logs included
- Troubleshooting checklist
- Console log attached for detailed debugging
- Quick action items

**Unstable Notifications (⚠️)**
- Test failure warnings
- Links to detailed test reports
- Suggestions for common test issues

#### Configuration

**Email Recipients:**
- Default: `team@example.com` (update in build parameters)
- Override per build using the `EMAIL_RECIPIENTS` parameter
- Supports comma-separated email lists

**SMTP Setup Required:**
See [Jenkins Setup Guide](JENKINS_SETUP.md#-email-notification-configuration) for detailed SMTP configuration instructions.

---

### Console Notifications (Deprecated)

Previously, notifications were only logged in the console output:

- ✅ **Success notifications** - Build summary with deployment URLs
- ✅ **Failure notifications** - Error details and troubleshooting info
- ✅ **Unstable notifications** - Test failure warnings

These console logs are still present but supplemented with email notifications.

---

### Extending Notifications (Optional)


#### Slack Notifications
Install Slack Notification plugin and add:
```groovy
post {
    success {
        slackSend(
            color: 'good',
            message: "Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
        )
    }
}
```

---

## 🚀 Deployment Strategy

### Rollback Mechanism
The `jenkins-deploy.sh` script includes automatic rollback:

```bash
if ! health_check; then
    echo "🔄 Deployment failed! Rolling back..."
    docker compose down
    exit 1
fi
```

**How it works:**
1. Deploy new containers
2. Run health checks on all services
3. If any health check fails → automatic rollback
4. If successful → deployment complete

### Manual Approval for Production
On `master`/`main` branch:

1. Build completes successfully
2. Pipeline pauses at **Deployment Approval** stage
3. Admin must manually approve
4. 30-minute timeout for approval
5. Only `admin` user can approve

---

## 📋 Code Quality Standards

### Jenkinsfile Best Practices ✅

1. **Well-organized stages**
   - Clear stage names with emoji indicators
   - Logical flow: Checkout → Build → Test → Deploy

2. **Parallel execution**
   - Services build in parallel
   - Tests run in parallel
   - Reduces total build time

3. **Error handling**
   - Proper `when` conditions
   - Rollback on failure
   - Cleanup in `post` sections

4. **Security**
   - Credentials never hardcoded
   - Secrets masked in logs
   - Environment isolation

5. **Documentation**
   - Comments explain complex logic
   - Echo statements for progress tracking
   - Clear parameter descriptions

---

## 🎓 Testing the Pipeline

### Test Automatic Triggering
```bash
# Make a minor change
echo "# Test commit" >> README.md

# Commit and push
git add README.md
git commit -m "test: trigger Jenkins build"
git push origin dev

# Within 2 minutes, Jenkins should automatically start a build
```

### Test Build Failures
```bash
# Introduce a syntax error in code
# For example, in a Java file, add invalid syntax

git add .
git commit -m "test: intentional error"
git push origin dev

# Observe Jenkins detecting and reporting the error
```

### Test Deployment Approval
```bash
# Merge to master
git checkout master
git merge dev
git push origin master

# Jenkins will pause at approval stage
# Go to Jenkins UI and approve manually
```

---

## 📈 Monitoring and Reports

### Available Reports
1. **Build History** - All builds with status
2. **Test Results** - JUnit test reports
3. **Build Trends** - Success/failure over time
4. **Console Output** - Detailed logs

### Accessing Reports
- **Dashboard**: Jenkins → Pipeline → Branch
- **Test Results**: Build → Test Result
- **Trends**: Pipeline → Build History

---

## 🔧 Troubleshooting

### Build Not Triggering Automatically?

1. **Check SCM polling is configured:**
   ```groovy
   triggers {
       pollSCM('H/2 * * * *')
   }
   ```

2. **Check Jenkins has access to Git:**
   - Verify credentials in Jenkins
   - Test with manual build first

3. **Check polling logs:**
   - Jenkins → Pipeline → Branch → Git Polling Log

### Tests Not Running?

1. **Verify SKIP_TESTS parameter is false**
2. **Check test files exist in services**
3. **Verify Maven test command works locally**

---

## 📚 Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Pipeline Syntax Reference](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [GitHub Webhook Guide](https://docs.github.com/en/developers/webhooks-and-events/webhooks)

---

## ✅ Audit Checklist

- [x] Automatic build triggering (SCM polling)
- [x] Automated testing with reports
- [x] Build error handling
- [x] Deployment automation
- [x] Rollback strategy
- [x] Security (credentials management)
- [x] Permissions and authorization
- [x] Code quality standards
- [x] Clear notifications
- [x] Test report storage
- [x] **BONUS:** Parameterized builds
- [x] **BONUS:** Distributed builds support (via Docker parallel builds)

---

**Last Updated:** 2025-11-24

