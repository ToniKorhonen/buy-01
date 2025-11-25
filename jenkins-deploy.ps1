# Jenkins Deployment Script for Windows
# PowerShell equivalent of jenkins-deploy.sh

# Set UTF-8 encoding for proper character display
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

# Don't stop on errors - we'll handle them manually
$ErrorActionPreference = "Continue"

Write-Host "🚀 Jenkins Deployment Script (Windows)" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$MAX_WAIT = 300  # 5 minutes max wait for health checks
$HEALTH_CHECK_INTERVAL = 10

# Function to check service health
function Test-ServiceHealth {
    param(
        [string]$ServiceName,
        [int]$Port
    )

    $maxAttempts = [math]::Floor($MAX_WAIT / $HEALTH_CHECK_INTERVAL)
    $attempt = 1

    Write-Host "⏳ Waiting for $ServiceName on port ${Port}..." -ForegroundColor Yellow

    while ($attempt -le $maxAttempts) {
        try {
            $tcpClient = New-Object System.Net.Sockets.TcpClient
            $connect = $tcpClient.BeginConnect("localhost", $Port, $null, $null)
            $wait = $connect.AsyncWaitHandle.WaitOne(1000, $false)

            if ($wait -and $tcpClient.Connected) {
                $tcpClient.Close()
                Write-Host "✅ $ServiceName is healthy" -ForegroundColor Green
                return $true
            }

            $tcpClient.Close()
        } catch {
            # Connection failed, continue waiting
        }

        if ($attempt -eq $maxAttempts) {
            Write-Host "❌ $ServiceName failed to start" -ForegroundColor Red
            return $false
        }

        Write-Host "   Attempt $attempt/$maxAttempts - waiting ${HEALTH_CHECK_INTERVAL}s..."
        Start-Sleep -Seconds $HEALTH_CHECK_INTERVAL
        $attempt++
    }

    return $false
}

# Function to rollback on failure
function Invoke-Rollback {
    Write-Host ""
    Write-Host "🔄 Deployment failed! Rolling back..." -ForegroundColor Red
    try {
        docker compose down 2>&1 | Out-Null
    } catch {
        Write-Host "Warning: Could not stop containers during rollback" -ForegroundColor Yellow
    }
    Write-Host "❌ Rollback complete. Please check the logs." -ForegroundColor Red
    exit 1
}

# Main deployment logic
try {
    Write-Host "📋 Step 1: Stopping existing containers..."
    $downResult = docker compose down 2>&1
    Write-Host "   (Stopped any existing containers)"

    Write-Host ""
    Write-Host "🐳 Step 2: Starting services with Docker Compose..."
    $upResult = docker compose up -d --build 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Docker compose failed with exit code: $LASTEXITCODE" -ForegroundColor Red
        Write-Host $upResult -ForegroundColor Red
        Invoke-Rollback
    }
    Write-Host "   Services started successfully"

    Write-Host ""
    Write-Host "🏥 Step 3: Running health checks..."
    Write-Host ""

    # Wait a bit for containers to initialize
    Start-Sleep -Seconds 5

    # Check MongoDB first (foundational service)
    if (-not (Test-ServiceHealth -ServiceName "MongoDB" -Port 27017)) {
        Invoke-Rollback
    }

    # Check backend services
    if (-not (Test-ServiceHealth -ServiceName "User Service" -Port 8081)) {
        Invoke-Rollback
    }

    if (-not (Test-ServiceHealth -ServiceName "Product Service" -Port 8082)) {
        Invoke-Rollback
    }

    if (-not (Test-ServiceHealth -ServiceName "Media Service" -Port 8083)) {
        Invoke-Rollback
    }

    # Check API Gateway
    if (-not (Test-ServiceHealth -ServiceName "API Gateway" -Port 8080)) {
        Invoke-Rollback
    }

    # Check Frontend
    if (-not (Test-ServiceHealth -ServiceName "Frontend (HTTP)" -Port 4200)) {
        Invoke-Rollback
    }

    if (-not (Test-ServiceHealth -ServiceName "Frontend (HTTPS)" -Port 4443)) {
        Invoke-Rollback
    }

    Write-Host ""
    Write-Host "==============================" -ForegroundColor Green
    Write-Host "✅ Deployment Successful!" -ForegroundColor Green
    Write-Host "==============================" -ForegroundColor Green
    Write-Host ""
    Write-Host "🌐 Application URLs:"
    Write-Host "   - Frontend (HTTPS): https://localhost:4443"
    Write-Host "   - Frontend (HTTP):  http://localhost:4200"
    Write-Host "   - API Gateway:      http://localhost:8080"
    Write-Host ""
    Write-Host "📊 Service Status:"
    docker compose ps
    Write-Host ""
    Write-Host "🎉 All services are running!" -ForegroundColor Green

    # Exit successfully
    exit 0

} catch {
    Write-Host ""
    Write-Host "Error during deployment: $($_.Exception.Message)" -ForegroundColor Red
    Invoke-Rollback
}

