pipeline {
    agent any

    environment {
        // Build configuration
        COMPOSE_PROJECT_NAME = 'buy01'
        BUILD_TIMESTAMP = sh(script: "date +%Y%m%d-%H%M%S", returnStdout: true).trim()
        GIT_COMMIT_SHORT = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()

        // Docker image tags
        IMAGE_TAG = "${env.BUILD_NUMBER}-${GIT_COMMIT_SHORT}"

        // Deployment flags based on branch
        SHOULD_DEPLOY = """${sh(
            returnStatus: true,
            script: '''
                if [ "${BRANCH_NAME}" = "main" ] || [ "${BRANCH_NAME}" = "master" ] || [ "${BRANCH_NAME}" = "dev" ]; then
                    exit 0
                else
                    exit 1
                fi
            '''
        ) == 0 ? 'true' : 'false'}"""

        NEEDS_APPROVAL = """${sh(
            returnStatus: true,
            script: 'test "${BRANCH_NAME}" = "main" || test "${BRANCH_NAME}" = "master"'
        ) == 0 ? 'true' : 'false'}"""
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "🔍 Building branch: ${env.BRANCH_NAME}"
                    echo "📦 Build number: ${env.BUILD_NUMBER}"
                    echo "🏷️  Image tag: ${IMAGE_TAG}"
                    echo "🚀 Should deploy: ${SHOULD_DEPLOY}"
                    echo "✋ Needs approval: ${NEEDS_APPROVAL}"
                }
                checkout scm
            }
        }

        stage('Environment Setup') {
            steps {
                script {
                    echo '🔧 Setting up environment...'

                    if (SHOULD_DEPLOY == 'true') {
                        // For deployment branches (main/dev): use Jenkins credentials
                        echo '📦 Creating .env with Jenkins credentials for deployment...'
                        withCredentials([
                            string(credentialsId: 'JWT_SECRET', variable: 'JWT_SECRET')
                        ]) {
                            sh '''
                                cat > .env << EOF
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
                                echo "✅ .env file created with Jenkins credentials"
                            '''
                        }
                    } else {
                        // For non-deployment branches: use dummy JWT for build testing
                        echo '🔨 Creating .env with test credentials for build-only...'
                        sh '''
                            cat > .env << EOF
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
                            echo "✅ .env file created with test credentials (build-only)"
                        '''
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
                            sh './mvnw clean package -DskipTests -Dmaven.javadoc.skip=true'
                        }
                    }
                }

                stage('Build Product Service') {
                    steps {
                        dir('Backend/product-service') {
                            echo '🔨 Building Product Service...'
                            sh './mvnw clean package -DskipTests -Dmaven.javadoc.skip=true'
                        }
                    }
                }

                stage('Build Media Service') {
                    steps {
                        dir('Backend/media-service') {
                            echo '🔨 Building Media Service...'
                            sh './mvnw clean package -DskipTests -Dmaven.javadoc.skip=true'
                        }
                    }
                }

                stage('Build API Gateway') {
                    steps {
                        dir('Backend/api-gateway') {
                            echo '🔨 Building API Gateway...'
                            sh './mvnw clean package -DskipTests -Dmaven.javadoc.skip=true'
                        }
                    }
                }

                stage('Build Frontend') {
                    steps {
                        dir('Frontend') {
                            echo '🔨 Building Frontend...'
                            sh '''
                                npm ci
                                npm run build -- --configuration=production
                            '''
                        }
                    }
                }
            }
        }

        stage('Build Docker Images') {
            when {
                expression { SHOULD_DEPLOY == 'true' }
            }
            steps {
                script {
                    echo '🐳 Building Docker images...'
                    sh """
                        # Build all images with docker compose
                        docker compose build --parallel

                        # Tag images with build number
                        docker tag buy01-user-service:latest buy01-user-service:${IMAGE_TAG}
                        docker tag buy01-product-service:latest buy01-product-service:${IMAGE_TAG}
                        docker tag buy01-media-service:latest buy01-media-service:${IMAGE_TAG}
                        docker tag buy01-api-gateway:latest buy01-api-gateway:${IMAGE_TAG}
                        docker tag buy01-frontend:latest buy01-frontend:${IMAGE_TAG}

                        echo "✅ Docker images built and tagged"
                    """
                }
            }
        }

        stage('Deployment Approval') {
            when {
                allOf {
                    expression { SHOULD_DEPLOY == 'true' }
                    expression { NEEDS_APPROVAL == 'true' }
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
                expression { SHOULD_DEPLOY == 'true' }
            }
            steps {
                script {
                    echo '🚀 Deploying application...'
                    sh '''
                        chmod +x jenkins-deploy.sh
                        ./jenkins-deploy.sh
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
            script {
                if (SHOULD_DEPLOY == 'true') {
                    echo """
                    🎉 Deployment successful!

                    Access the application at:
                    - Frontend (HTTPS): https://localhost:4443
                    - Frontend (HTTP): http://localhost:4200
                    - API Gateway: http://localhost:8080

                    Build: ${env.BUILD_NUMBER}
                    Tag: ${IMAGE_TAG}
                    Branch: ${env.BRANCH_NAME}
                    """
                }
            }
        }
        failure {
            echo '❌ Pipeline failed!'
            script {
                sh 'docker compose logs --tail=50 || true'
            }
        }
        cleanup {
            echo '🧹 Cleaning up workspace...'
            sh '''
                # Clean up .env file (contains secrets)
                rm -f .env

                # Clean node_modules to save space (optional)
                # rm -rf Frontend/node_modules
            '''
        }
    }
}

