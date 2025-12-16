pipeline {
    agent any

    environment {
        // Build configuration
        COMPOSE_PROJECT_NAME = 'buy01'
        // Platform-agnostic variables - set dynamically in Checkout stage
    }

    // Automatic build triggers - AUDIT REQUIREMENT: Auto-trigger on commit
    triggers {
        // Poll SCM every 2 minutes for changes
        pollSCM('H/2 * * * *')
    }

    // Parameterized builds - BONUS REQUIREMENT
    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['auto', 'dev', 'prod'],
            description: 'Override deployment environment (auto = based on branch)'
        )
        booleanParam(
            name: 'SKIP_TESTS',
            defaultValue: false,
            description: 'Skip test execution (not recommended for production)'
        )
        booleanParam(
            name: 'CLEAN_BUILD',
            defaultValue: false,
            description: 'Force clean build (remove all Docker caches)'
        )
        string(
            name: 'EMAIL_RECIPIENTS',
            defaultValue: 'team@example.com',
            description: 'Comma-separated email addresses for build notifications'
        )
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm

                script {
                    // Initialize environment variables in a cross-platform way
                    def buildTimestamp = ''
                    def gitCommitShort = ''

                    try {
                        if (isUnix()) {
                            buildTimestamp = sh(script: 'date +%Y%m%d-%H%M%S', returnStdout: true).trim()
                            gitCommitShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        } else {
                            // Windows: Use PowerShell with explicit output encoding
                            buildTimestamp = powershell(
                                script: '[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; Get-Date -Format "yyyyMMdd-HHmmss"',
                                returnStdout: true
                            ).trim()

                            gitCommitShort = powershell(
                                script: '[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; git rev-parse --short HEAD',
                                returnStdout: true
                            ).trim()
                        }
                    } catch (Exception e) {
                        echo "⚠️  Warning: Could not retrieve git info: ${e.message}"
                        buildTimestamp = "unknown"
                        gitCommitShort = "unknown"
                    }

                    // Debug: Print local variables
                    echo "DEBUG: buildTimestamp = ${buildTimestamp}"
                    echo "DEBUG: gitCommitShort = ${gitCommitShort}"

                    // Set environment variables with proper null handling
                    env.BUILD_TIMESTAMP = buildTimestamp ?: "unknown-${env.BUILD_NUMBER}"
                    env.GIT_COMMIT_SHORT = gitCommitShort ?: "unknown"
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"

                    // Determine deployment flags based on branch
                    def deployBranches = ['main', 'master', 'dev']
                    def approvalBranches = ['main', 'master']

                    env.SHOULD_DEPLOY = (deployBranches.contains(env.BRANCH_NAME)) ? 'true' : 'false'
                    env.NEEDS_APPROVAL = (approvalBranches.contains(env.BRANCH_NAME)) ? 'true' : 'false'

                    echo "🔍 Building branch: ${env.BRANCH_NAME}"
                    echo "📦 Build number: ${env.BUILD_NUMBER}"
                    echo "🕒 Build timestamp: ${env.BUILD_TIMESTAMP}"
                    echo "🏷️  Image tag: ${env.IMAGE_TAG}"
                    echo "🚀 Should deploy: ${env.SHOULD_DEPLOY}"
                    echo "✋ Needs approval: ${env.NEEDS_APPROVAL}"
                }
            }
        }

        stage('Environment Setup') {
            steps {
                script {
                    echo '🔧 Setting up environment...'

                    if (env.SHOULD_DEPLOY == 'true') {
                        // For deployment branches (main/dev): use Jenkins credentials
                        echo '📦 Creating .env with Jenkins credentials for deployment...'
                        withCredentials([
                            string(credentialsId: 'JWT_SECRET', variable: 'JWT_SECRET')
                        ]) {
                            // Use shell/bat to write file securely without exposing secret in logs
                            if (isUnix()) {
                                sh '''
cat > .env << 'EOF'
# JWT Configuration (from Jenkins credentials)
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=3600000

# MongoDB Configuration
MONGODB_HOST=mongodb
MONGODB_PORT=27017

# Database Names
USER_DB_NAME=buy01_users
PRODUCT_DB_NAME=buy01_products
MEDIA_DB_NAME=media_db
EOF
                                '''
                            } else {
                                bat '''
@echo off
(
echo # JWT Configuration (from Jenkins credentials^)
echo JWT_SECRET=%JWT_SECRET%
echo JWT_EXPIRATION=3600000
echo.
echo # MongoDB Configuration
echo MONGODB_HOST=mongodb
echo MONGODB_PORT=27017
echo.
echo # Database Names
echo USER_DB_NAME=buy01_users
echo PRODUCT_DB_NAME=buy01_products
echo MEDIA_DB_NAME=media_db
) > .env
                                '''
                            }
                            echo "✅ .env file created with Jenkins credentials"
                        }
                    } else {
                        // For non-deployment branches: use dummy JWT for build testing
                        echo '🔨 Creating .env with test credentials for build-only...'
                        if (isUnix()) {
                            sh '''
cat > .env << 'EOF'
# JWT Configuration (test credentials - not for production)
JWT_SECRET=test-jwt-secret-for-build-only-not-for-deployment-12345
JWT_EXPIRATION=3600000

# MongoDB Configuration
MONGODB_HOST=mongodb
MONGODB_PORT=27017

# Database Names
USER_DB_NAME=buy01_users
PRODUCT_DB_NAME=buy01_products
MEDIA_DB_NAME=media_db
EOF
                            '''
                        } else {
                            bat '''
@echo off
(
echo # JWT Configuration (test credentials - not for production^)
echo JWT_SECRET=test-jwt-secret-for-build-only-not-for-deployment-12345
echo JWT_EXPIRATION=3600000
echo.
echo # MongoDB Configuration
echo MONGODB_HOST=mongodb
echo MONGODB_PORT=27017
echo.
echo # Database Names
echo USER_DB_NAME=buy01_users
echo PRODUCT_DB_NAME=buy01_products
echo MEDIA_DB_NAME=media_db
) > .env
                            '''
                        }
                        echo "✅ .env file created with test credentials (build-only)"
                    }
                }
            }
        }

        stage('Build Services') {
            parallel {
                stage('Build User Service') {
                    steps {
                        dir('Backend/user-service') {
                            echo '🔨 Building User Service...'
                            script {
                                if (isUnix()) {
                                    sh './mvnw clean package -DskipTests -Dmaven.javadoc.skip=true'
                                } else {
                                    bat 'mvnw.cmd clean package -DskipTests -Dmaven.javadoc.skip=true'
                                }
                            }
                        }
                    }
                }

                stage('Build Product Service') {
                    steps {
                        dir('Backend/product-service') {
                            echo '🔨 Building Product Service...'
                            script {
                                if (isUnix()) {
                                    sh './mvnw clean package -DskipTests -Dmaven.javadoc.skip=true'
                                } else {
                                    bat 'mvnw.cmd clean package -DskipTests -Dmaven.javadoc.skip=true'
                                }
                            }
                        }
                    }
                }

                stage('Build Media Service') {
                    steps {
                        dir('Backend/media-service') {
                            echo '🔨 Building Media Service...'
                            script {
                                if (isUnix()) {
                                    sh './mvnw clean package -DskipTests -Dmaven.javadoc.skip=true'
                                } else {
                                    bat 'mvnw.cmd clean package -DskipTests -Dmaven.javadoc.skip=true'
                                }
                            }
                        }
                    }
                }

                stage('Build API Gateway') {
                    steps {
                        dir('Backend/api-gateway') {
                            echo '🔨 Building API Gateway...'
                            script {
                                if (isUnix()) {
                                    sh './mvnw clean package -DskipTests -Dmaven.javadoc.skip=true'
                                } else {
                                    bat 'mvnw.cmd clean package -DskipTests -Dmaven.javadoc.skip=true'
                                }
                            }
                        }
                    }
                }

                stage('Build Frontend') {
                    steps {
                        dir('Frontend') {
                            echo '🔨 Building Frontend...'
                            script {
                                if (isUnix()) {
                                    sh '''
                                        npm ci
                                        npm run build -- --configuration=production
                                    '''
                                } else {
                                    bat '''
                                        npm ci
                                        npm run build -- --configuration=production
                                    '''
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Run Tests') {
            when {
                expression { params.SKIP_TESTS == false }
            }
            environment {
                JWT_SECRET = 'test-jwt-secret-for-testing-only-do-not-use-in-production-12345678901234567890'
            }
            parallel {
                stage('Test User Service') {
                    steps {
                        dir('Backend/user-service') {
                            echo '🧪 Testing User Service...'
                            script {
                                if (isUnix()) {
                                    sh '''
                                        export JWT_SECRET="${JWT_SECRET}"
                                        ./mvnw test
                                    '''
                                } else {
                                    bat '''
                                        set JWT_SECRET=%JWT_SECRET%
                                        mvnw.cmd test
                                    '''
                                }
                            }
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'Backend/user-service/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Test Product Service') {
                    steps {
                        dir('Backend/product-service') {
                            echo '🧪 Testing Product Service...'
                            script {
                                if (isUnix()) {
                                    sh '''
                                        export JWT_SECRET="${JWT_SECRET}"
                                        ./mvnw test
                                    '''
                                } else {
                                    bat '''
                                        set JWT_SECRET=%JWT_SECRET%
                                        mvnw.cmd test
                                    '''
                                }
                            }
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'Backend/product-service/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Test API Gateway') {
                    steps {
                        dir('Backend/api-gateway') {
                            echo '🧪 Testing API Gateway...'
                            script {
                                if (isUnix()) {
                                    sh '''
                                        export JWT_SECRET="${JWT_SECRET}"
                                        ./mvnw test
                                    '''
                                } else {
                                    bat '''
                                        set JWT_SECRET=%JWT_SECRET%
                                        mvnw.cmd test
                                    '''
                                }
                            }
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'Backend/api-gateway/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Test Media Service') {
                    steps {
                        dir('Backend/media-service') {
                            echo '🧪 Testing Media Service...'
                            script {
                                if (isUnix()) {
                                    sh '''
                                        export JWT_SECRET="${JWT_SECRET}"
                                        ./mvnw test
                                    '''
                                } else {
                                    bat '''
                                        set JWT_SECRET=%JWT_SECRET%
                                        mvnw.cmd test
                                    '''
                                }
                            }
                        }
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'Backend/media-service/target/surefire-reports/*.xml'
                        }
                    }
                }
            }
        }

        // AUDIT REQUIREMENT: Publish Test Reports and Coverage
        stage('Publish Test Reports') {
            when {
                expression { params.SKIP_TESTS == false }
            }
            steps {
                script {
                    echo '📊 Publishing test coverage reports...'

                    // Publish JaCoCo coverage reports using Coverage plugin
                    try {
                        // The Coverage plugin automatically picks up JaCoCo reports
                        publishCoverage adapters: [
                            jacocoAdapter(
                                path: '**/target/site/jacoco/jacoco.xml',
                                thresholds: [
                                    [thresholdTarget: 'Line', unhealthyThreshold: 50.0, unstableThreshold: 70.0],
                                    [thresholdTarget: 'Conditional', unhealthyThreshold: 50.0, unstableThreshold: 70.0]
                                ]
                            )
                        ],
                        sourceFileResolver: sourceFiles('STORE_ALL_BUILD')
                        echo '✅ Coverage reports published via Coverage plugin'
                    } catch (Exception e) {
                        echo "⚠️  Coverage plugin not installed or reports not found: ${e.message}"
                        echo "Install Coverage plugin from: Manage Jenkins → Plugins → Code Coverage API"
                    }

                    // Archive HTML coverage reports for each service
                    try {
                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'Backend/user-service/target/site/jacoco',
                            reportFiles: 'index.html',
                            reportName: 'User Service Coverage',
                            reportTitles: 'User Service Code Coverage'
                        ])
                        echo '✅ User Service coverage report published'
                    } catch (Exception e) {
                        echo "⚠️  HTML Publisher plugin not installed: ${e.message}"
                        echo "Install HTML Publisher from: Manage Jenkins → Plugins → HTML Publisher"
                    }

                    try {
                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'Backend/product-service/target/site/jacoco',
                            reportFiles: 'index.html',
                            reportName: 'Product Service Coverage',
                            reportTitles: 'Product Service Code Coverage'
                        ])
                        echo '✅ Product Service coverage report published'
                    } catch (Exception e) {
                        echo "⚠️  Could not publish Product Service coverage: ${e.message}"
                    }

                    try {
                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'Backend/api-gateway/target/site/jacoco',
                            reportFiles: 'index.html',
                            reportName: 'API Gateway Coverage',
                            reportTitles: 'API Gateway Code Coverage'
                        ])
                        echo '✅ API Gateway coverage report published'
                    } catch (Exception e) {
                        echo "⚠️  Could not publish API Gateway coverage: ${e.message}"
                    }

                    try {
                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'Backend/media-service/target/site/jacoco',
                            reportFiles: 'index.html',
                            reportName: 'Media Service Coverage',
                            reportTitles: 'Media Service Code Coverage'
                        ])
                        echo '✅ Media Service coverage report published'
                    } catch (Exception e) {
                        echo "⚠️  Could not publish Media Service coverage: ${e.message}"
                    }
                }
            }
        }

        // AUDIT REQUIREMENT: Code Quality Analysis with SonarCloud
        stage('SonarCloud Analysis') {
            when {
                expression {
                    // Only run SonarCloud on deployment branches to save analysis quota
                    return env.SHOULD_DEPLOY == 'true' || params.DEPLOY_ENV != 'auto'
                }
            }
            environment {
                // SonarCloud uses SONAR_TOKEN from Jenkins credentials
                SONAR_SCANNER_OPTS = '-Xmx512m'
                SONAR_ORGANIZATION = 'tonikorhonen'
                SONAR_PROJECT_KEY = 'ToniKorhonen_buy-01'
            }
            steps {
                echo '📊 Analyzing entire monorepo with SonarCloud...'
                script {
                    // Catch SonarCloud configuration errors gracefully
                    try {
                        withSonarQubeEnv('SonarCloud') {
                            if (isUnix()) {
                                sh '''
                                    # Install sonar-scanner if not available
                                    if ! command -v sonar-scanner &> /dev/null; then
                                        echo "Installing sonar-scanner..."
                                        npm install -g sonarqube-scanner
                                    fi

                                    # Prepare Java libraries paths for better analysis
                                    USER_SERVICE_LIBS="Backend/user-service/target/classes:Backend/user-service/target/user-0.0.1-SNAPSHOT.jar"
                                    PRODUCT_SERVICE_LIBS="Backend/product-service/target/classes:Backend/product-service/target/product-0.0.1-SNAPSHOT.jar"
                                    MEDIA_SERVICE_LIBS="Backend/media-service/target/classes:Backend/media-service/target/media-0.0.1-SNAPSHOT.jar"
                                    GATEWAY_LIBS="Backend/api-gateway/target/classes:Backend/api-gateway/target/gateway-0.0.1-SNAPSHOT.jar"

                                    # Collect all Maven dependencies
                                    MAVEN_REPO="$HOME/.m2/repository"

                                    # Run sonar-scanner from project root using sonar-project.properties
                                    sonar-scanner \
                                        -Dsonar.organization=${SONAR_ORGANIZATION} \
                                        -Dsonar.host.url=https://sonarcloud.io \
                                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                        -Dsonar.java.binaries="Backend/*/target/classes" \
                                        -Dsonar.java.libraries="Backend/*/target/*.jar,$MAVEN_REPO/**/*.jar" \
                                        -Dsonar.verbose=true
                                '''
                            } else {
                                bat '''
                                    @echo off
                                    where sonar-scanner >nul 2>&1
                                    if %ERRORLEVEL% NEQ 0 (
                                        echo Installing sonar-scanner...
                                        npm install -g sonarqube-scanner
                                    )

                                    REM Run sonar-scanner from project root using sonar-project.properties
                                    sonar-scanner ^
                                        -Dsonar.organization=%SONAR_ORGANIZATION% ^
                                        -Dsonar.host.url=https://sonarcloud.io ^
                                        -Dsonar.projectKey=%SONAR_PROJECT_KEY% ^
                                        -Dsonar.java.binaries=Backend/*/target/classes ^
                                        -Dsonar.java.libraries=Backend/*/target/*.jar ^
                                        -Dsonar.verbose=true
                                '''
                            }
                        }
                    } catch (Exception e) {
                        echo "⚠️  SonarCloud analysis failed: ${e.message}"
                        echo """
                        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        📋 SonarCloud Configuration Required
                        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                        To enable SonarCloud analysis, configure it in Jenkins:

                        1. Go to: Jenkins → Manage Jenkins → Configure System
                        2. Find "SonarQube servers" section
                        3. Click "Add SonarQube"
                        4. Configure:
                           • Name: SonarCloud
                           • Server URL: https://sonarcloud.io
                           • Server authentication token: [Add your SonarCloud token]

                        5. Get your token from: https://sonarcloud.io/account/security

                        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        """
                        // Don't fail the build if SonarCloud is not configured
                        unstable('SonarCloud analysis skipped - not configured')
                    }
                }
            }
        }

        stage('Build Docker Images') {
            when {
                allOf {
                    expression { env.SHOULD_DEPLOY == 'true' }
                    expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
                }
            }
            steps {
                script {
                    echo '🐳 Building Docker images...'

                    def imageTag = env.IMAGE_TAG ?: "${env.BUILD_NUMBER}-unknown"

                    if (isUnix()) {
                        sh """
                            # Build all images with docker compose
                            docker compose build --parallel

                            # Tag images with build number
                            docker tag buy01-user-service:latest buy01-user-service:${imageTag}
                            docker tag buy01-product-service:latest buy01-product-service:${imageTag}
                            docker tag buy01-media-service:latest buy01-media-service:${imageTag}
                            docker tag buy01-api-gateway:latest buy01-api-gateway:${imageTag}
                            docker tag buy01-frontend:latest buy01-frontend:${imageTag}

                            echo "✅ Docker images built and tagged"
                        """
                    } else {
                        bat """
                            @echo off
                            echo Building all images with docker compose
                            docker compose build --parallel

                            echo Tagging images with build number
                            docker tag buy01-user-service:latest buy01-user-service:${imageTag}
                            docker tag buy01-product-service:latest buy01-product-service:${imageTag}
                            docker tag buy01-media-service:latest buy01-media-service:${imageTag}
                            docker tag buy01-api-gateway:latest buy01-api-gateway:${imageTag}
                            docker tag buy01-frontend:latest buy01-frontend:${imageTag}

                            echo Docker images built and tagged
                        """
                    }
                }
            }
        }

        stage('Deployment Approval') {
            when {
                allOf {
                    expression { env.SHOULD_DEPLOY == 'true' }
                    expression { env.NEEDS_APPROVAL == 'true' }
                    expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
                }
            }
            steps {
                script {
                    echo '⏸️  Waiting for deployment approval...'
                    timeout(time: 30, unit: 'MINUTES') {
                        input message: 'Deploy to production?',
                              ok: 'Deploy',
                              submitter: 'admin'
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                allOf {
                    expression { env.SHOULD_DEPLOY == 'true' }
                    expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
                }
            }
            steps {
                script {
                    echo '🚀 Deploying application...'

                    if (isUnix()) {
                        sh '''
                            chmod +x jenkins-deploy.sh
                            ./jenkins-deploy.sh
                        '''
                    } else {
                        powershell '''
                            Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
                            .\\jenkins-deploy.ps1
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                // AUDIT REQUIREMENT: Archive test reports and coverage data for future reference
                echo '📦 Archiving test reports and coverage data...'
                try {
                    archiveArtifacts(
                        artifacts: '**/target/surefire-reports/*.xml, **/target/site/jacoco/**/*',
                        allowEmptyArchive: true,
                        fingerprint: true
                    )
                    echo '✅ Test reports and coverage data archived'
                } catch (Exception e) {
                    echo "⚠️  Could not archive artifacts: ${e.message}"
                }
            }
        }

        success {
            echo '✅ Pipeline completed successfully!'
            script {
                def deploymentInfo = ''
                if (env.SHOULD_DEPLOY == 'true') {
                    echo """
                    🎉 Deployment successful!

                    Access the application at:
                    - Frontend (HTTPS): https://localhost:4443
                    - Frontend (HTTP): http://localhost:4200
                    - API Gateway: http://localhost:8080

                    Build: ${env.BUILD_NUMBER}
                    Tag: ${env.IMAGE_TAG ?: 'unknown'}
                    Branch: ${env.BRANCH_NAME}
                    """

                    deploymentInfo = """
                    <h3>🚀 Deployment Information</h3>
                    <ul>
                        <li><strong>Frontend (HTTPS):</strong> <a href="https://localhost:4443">https://localhost:4443</a></li>
                        <li><strong>Frontend (HTTP):</strong> <a href="http://localhost:4200">http://localhost:4200</a></li>
                        <li><strong>API Gateway:</strong> <a href="http://localhost:8080">http://localhost:8080</a></li>
                        <li><strong>Image Tag:</strong> ${env.IMAGE_TAG ?: 'unknown'}</li>
                    </ul>
                    """
                }

                // AUDIT REQUIREMENT: Notifications on success
                echo """
                ✅ Build Summary:
                - Status: SUCCESS
                - Branch: ${env.BRANCH_NAME}
                - Build: #${env.BUILD_NUMBER}
                - Commit: ${env.GIT_COMMIT_SHORT ?: 'unknown'}
                - Duration: ${currentBuild.durationString}
                """

                // Email notification on success
                emailext(
                    subject: "✅ BUILD SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER} [${env.BRANCH_NAME}]",
                    body: """
                    <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h2 style="color: #28a745;">✅ Build Successful</h2>

                        <h3>📋 Build Details</h3>
                        <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                            <tr><td><strong>Project</strong></td><td>${env.JOB_NAME}</td></tr>
                            <tr><td><strong>Build Number</strong></td><td>#${env.BUILD_NUMBER}</td></tr>
                            <tr><td><strong>Branch</strong></td><td>${env.BRANCH_NAME}</td></tr>
                            <tr><td><strong>Commit</strong></td><td>${env.GIT_COMMIT_SHORT ?: 'unknown'}</td></tr>
                            <tr><td><strong>Build Timestamp</strong></td><td>${env.BUILD_TIMESTAMP ?: 'unknown'}</td></tr>
                            <tr><td><strong>Duration</strong></td><td>${currentBuild.durationString}</td></tr>
                        </table>

                        ${deploymentInfo}

                        <h3>🔗 Links</h3>
                        <ul>
                            <li><a href="${env.BUILD_URL}">View Build Console Output</a></li>
                            <li><a href="${env.BUILD_URL}testReport/">View Test Reports</a></li>
                            <li><a href="${env.BUILD_URL}jacoco/">View Code Coverage Summary</a></li>
                            <li><a href="${env.BUILD_URL}User_Service_Coverage/">User Service Coverage Report</a></li>
                            <li><a href="${env.BUILD_URL}Product_Service_Coverage/">Product Service Coverage Report</a></li>
                            <li><a href="${env.BUILD_URL}API_Gateway_Coverage/">API Gateway Coverage Report</a></li>
                        </ul>

                        <p style="color: #6c757d; font-size: 12px;">
                            This is an automated notification from Jenkins CI/CD pipeline.
                        </p>
                    </body>
                    </html>
                    """,
                    to: "${params.EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }

        failure {
            echo '❌ Pipeline failed!'
            script {
                def dockerLogs = ''
                // Show docker logs if available
                try {
                    if (isUnix()) {
                        dockerLogs = sh(script: 'docker compose logs --tail=50 2>&1 || echo "No docker logs available"', returnStdout: true).trim()
                        sh 'docker compose logs --tail=50 || true'
                    } else {
                        dockerLogs = bat(script: '@echo off && docker compose logs --tail=50 2>&1 || echo No docker logs available', returnStdout: true).trim()
                        bat 'docker compose logs --tail=50 || exit /b 0'
                    }
                } catch (Exception e) {
                    echo "Could not retrieve docker logs: ${e.message}"
                    dockerLogs = "Could not retrieve docker logs: ${e.message}"
                }

                // AUDIT REQUIREMENT: Notifications on failure
                echo """
                ❌ Build Failed:
                - Branch: ${env.BRANCH_NAME}
                - Build: #${env.BUILD_NUMBER}
                - Commit: ${env.GIT_COMMIT_SHORT ?: 'unknown'}
                - Stage: ${env.STAGE_NAME ?: 'Unknown'}

                Check console output for details.
                """

                // Email notification on failure
                emailext(
                    subject: "❌ BUILD FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER} [${env.BRANCH_NAME}]",
                    body: """
                    <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h2 style="color: #dc3545;">❌ Build Failed</h2>

                        <h3>📋 Build Details</h3>
                        <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                            <tr><td><strong>Project</strong></td><td>${env.JOB_NAME}</td></tr>
                            <tr><td><strong>Build Number</strong></td><td>#${env.BUILD_NUMBER}</td></tr>
                            <tr><td><strong>Branch</strong></td><td>${env.BRANCH_NAME}</td></tr>
                            <tr><td><strong>Commit</strong></td><td>${env.GIT_COMMIT_SHORT ?: 'unknown'}</td></tr>
                            <tr><td><strong>Failed Stage</strong></td><td style="color: #dc3545;"><strong>${env.STAGE_NAME ?: 'Unknown'}</strong></td></tr>
                            <tr><td><strong>Build Timestamp</strong></td><td>${env.BUILD_TIMESTAMP ?: 'unknown'}</td></tr>
                            <tr><td><strong>Duration</strong></td><td>${currentBuild.durationString}</td></tr>
                        </table>

                        <h3>🔍 Troubleshooting</h3>
                        <ul>
                            <li><a href="${env.BUILD_URL}console">View Full Console Output</a></li>
                            <li><a href="${env.BUILD_URL}testReport/">View Test Reports</a></li>
                            <li>Check the failed stage: <strong>${env.STAGE_NAME ?: 'Unknown'}</strong></li>
                        </ul>

                        <h3>🐳 Docker Logs (Last 50 lines)</h3>
                        <pre style="background-color: #f4f4f4; padding: 10px; border: 1px solid #ddd; overflow: auto; max-height: 300px;">
${dockerLogs.take(5000)}
                        </pre>

                        <h3>⚡ Quick Actions</h3>
                        <ul>
                            <li>Review the console output for detailed error messages</li>
                            <li>Check if all required services are running</li>
                            <li>Verify JWT_SECRET credential is configured</li>
                            <li>Ensure Docker daemon is running</li>
                            <li>Check for port conflicts (4200, 4443, 8080, 27017)</li>
                        </ul>

                        <p style="color: #6c757d; font-size: 12px;">
                            This is an automated notification from Jenkins CI/CD pipeline.
                        </p>
                    </body>
                    </html>
                    """,
                    to: "${params.EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: true
                )
            }
        }

        unstable {
            echo '⚠️  Pipeline unstable (tests may have failed)'
            script {
                echo """
                ⚠️  Build Unstable:
                - Some tests may have failed
                - Check test reports for details
                - Branch: ${env.BRANCH_NAME}
                - Build: #${env.BUILD_NUMBER}
                """

                // Email notification on unstable build
                emailext(
                    subject: "⚠️ BUILD UNSTABLE: ${env.JOB_NAME} #${env.BUILD_NUMBER} [${env.BRANCH_NAME}]",
                    body: """
                    <html>
                    <body style="font-family: Arial, sans-serif;">
                        <h2 style="color: #ffc107;">⚠️ Build Unstable</h2>
                        <p>The build completed, but some tests may have failed or the build is unstable.</p>

                        <h3>📋 Build Details</h3>
                        <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                            <tr><td><strong>Project</strong></td><td>${env.JOB_NAME}</td></tr>
                            <tr><td><strong>Build Number</strong></td><td>#${env.BUILD_NUMBER}</td></tr>
                            <tr><td><strong>Branch</strong></td><td>${env.BRANCH_NAME}</td></tr>
                            <tr><td><strong>Commit</strong></td><td>${env.GIT_COMMIT_SHORT ?: 'unknown'}</td></tr>
                            <tr><td><strong>Build Timestamp</strong></td><td>${env.BUILD_TIMESTAMP ?: 'unknown'}</td></tr>
                            <tr><td><strong>Duration</strong></td><td>${currentBuild.durationString}</td></tr>
                        </table>

                        <h3>🧪 Test Results</h3>
                        <p>Please review the test reports to identify which tests failed:</p>
                        <ul>
                            <li><a href="${env.BUILD_URL}testReport/">View Detailed Test Reports</a></li>
                            <li><a href="${env.BUILD_URL}console">View Console Output</a></li>
                        </ul>

                        <h3>💡 Possible Causes</h3>
                        <ul>
                            <li>Unit test failures in User Service, Product Service, or API Gateway</li>
                            <li>Integration test failures</li>
                            <li>Test timeouts or flaky tests</li>
                            <li>Environment-specific test issues</li>
                        </ul>

                        <p style="color: #6c757d; font-size: 12px;">
                            This is an automated notification from Jenkins CI/CD pipeline.
                        </p>
                    </body>
                    </html>
                    """,
                    to: "${params.EMAIL_RECIPIENTS}",
                    mimeType: 'text/html',
                    attachLog: false
                )
            }
        }

        cleanup {
            echo '🧹 Cleaning up workspace...'
            script {
                try {
                    if (isUnix()) {
                        sh '''
                            # Clean up .env file (contains secrets)
                            rm -f .env

                            # Clean node_modules to save space (optional)
                            # rm -rf Frontend/node_modules
                        '''
                    } else {
                        bat '''
                            @echo off
                            REM Clean up .env file (contains secrets)
                            if exist .env del /f .env

                            REM Clean node_modules to save space (optional)
                            REM if exist Frontend\\node_modules rmdir /s /q Frontend\\node_modules
                        '''
                    }
                } catch (Exception e) {
                    echo "Cleanup warning: ${e.message}"
                }
            }
        }
    }
}


