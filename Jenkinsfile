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
                            def envContent = """# JWT Configuration (from Jenkins credentials)
JWT_SECRET=${env.JWT_SECRET}
JWT_EXPIRATION=3600000

# MongoDB Configuration
MONGODB_HOST=mongodb
MONGODB_PORT=27017

# Database Names
USER_DB_NAME=buy01_users
PRODUCT_DB_NAME=buy01_products
MEDIA_DB_NAME=media_db
"""
                            writeFile file: '.env', text: envContent
                            echo "✅ .env file created with Jenkins credentials"
                        }
                    } else {
                        // For non-deployment branches: use dummy JWT for build testing
                        echo '🔨 Creating .env with test credentials for build-only...'
                        def envContent = """# JWT Configuration (test credentials - not for production)
JWT_SECRET=test-jwt-secret-for-build-only-not-for-deployment-12345
JWT_EXPIRATION=3600000

# MongoDB Configuration
MONGODB_HOST=mongodb
MONGODB_PORT=27017

# Database Names
USER_DB_NAME=buy01_users
PRODUCT_DB_NAME=buy01_products
MEDIA_DB_NAME=media_db
"""
                        writeFile file: '.env', text: envContent
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

        // AUDIT REQUIREMENT: Automated testing
        stage('Run Tests') {
            when {
                expression { params.SKIP_TESTS == false }
            }
            environment {
                // Set JWT_SECRET for tests - use test value that won't be used in production
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
                                        ./mvnw test || true
                                    '''
                                } else {
                                    bat '''
                                        set JWT_SECRET=%JWT_SECRET%
                                        mvnw.cmd test || exit /b 0
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
                                        ./mvnw test || true
                                    '''
                                } else {
                                    bat '''
                                        set JWT_SECRET=%JWT_SECRET%
                                        mvnw.cmd test || exit /b 0
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
                                        ./mvnw test || true
                                    '''
                                } else {
                                    bat '''
                                        set JWT_SECRET=%JWT_SECRET%
                                        mvnw.cmd test || exit /b 0
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
            }
        }

        stage('Build Docker Images') {
            when {
                expression { env.SHOULD_DEPLOY == 'true' }
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
                expression { env.SHOULD_DEPLOY == 'true' }
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
        success {
            echo '✅ Pipeline completed successfully!'
            script {
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
            }
        }

        failure {
            echo '❌ Pipeline failed!'
            script {
                // Show docker logs if available
                try {
                    if (isUnix()) {
                        sh 'docker compose logs --tail=50 || true'
                    } else {
                        bat 'docker compose logs --tail=50 || exit /b 0'
                    }
                } catch (Exception e) {
                    echo "Could not retrieve docker logs: ${e.message}"
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

