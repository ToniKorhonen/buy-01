#!/usr/bin/env pwsh
# Cross-platform Jenkins Deployment Script (Windows + Linux)

# Force UTF-8 everywhere (important in Jenkins)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$ErrorActionPreference = "Stop"

Write-Host "🚀 Jenkins Deployment Script (Cross-Platform)"
Write-Host "==========================================="
Write-Host ""

# Detect platform
$IS_WINDOWS = $PSVersionTable.OS -match "Windows"

# Config
$MAX_WAIT = 300
$HEALTH_CHECK_INTERVAL = 10


# ---------------------------------------------------------
# Health Check Function (Cross-platform TCP test)
# ---------------------------------------------------------
function Test-ServiceHealth {
    param(
        [string]$ServiceName,
        [int]$Port
    )

    $maxAttempts = [math]::Floor($MAX_WAIT / $HEALTH_CHECK_INTERVAL)

    Write-Host "⏳ Waiting for $ServiceName on port $Port..." -ForegroundColor Yellow

    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {

        try {
            # Windows has Test-NetConnection
            if ($IS_WINDOWS -and (Get-Command Test-NetConnection -ErrorAction SilentlyContinue)) {
                $result = Test-NetConnection -ComputerName "localhost" -Port $Port
                if ($result.TcpTestSucceeded) {
                    Write-Host "✅ $ServiceName is healthy" -ForegroundColor Green
                    return $true
                }
            }
            else {
                # Linux / MacOS / fallback
                $client = New-Object System.Net.Sockets.TcpClient
                $iar = $client.BeginConnect("localhost", $Port, $null, $null)
                $connected = $iar.AsyncWaitHandle.WaitOne(1000, $false)

                if ($connected -and $client.Connected) {
                    $client.Close()
                    Write-Host "✅ $ServiceName is healthy" -ForegroundColor Green
                    return $true
                }
                $client.Close()
            }
        } catch {
            # ignore connection failure
        }

        if ($attempt -eq $maxAttempts) {
            Write-Host "❌ $ServiceName failed to start" -ForegroundColor Red
            return $false
        }

        Write-Host "   Attempt $attempt/$maxAttempts - waiting ${HEALTH_CHECK_INTERVAL}s..."
        Start-Sleep -Seconds $HEALTH_CHECK_INTERVAL
    }

    return $false
}

# ---------------------------------------------------------
# Rollback
# ---------------------------------------------------------
function Invoke-Rollback {
    Write-Host ""
    Write-Host "🔄 Deployment failed! Rolling back..." -ForegroundColor Red
    try {
        docker compose down *>$null
    } catch {
        Write-Host "Warning: Could not stop containers during rollback" -ForegroundColor Yellow
    }
    Write-Host "❌ Rollback complete. Please check logs." -ForegroundColor Red
    exit 1
}


# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------
try {

    Write-Host "📋 Step 1: Stopping existing containers..."
    docker compose down *>$null
    Write-Host "   ✅ Containers stopped"

    Write-Host ""
    Write-Host "🐳 Step 2: Starting services with Docker Compose..."
    $upOutput = docker compose up -d --build 2>&1 | Out-String

    if ($LASTEXITCODE -ne 0) {
        Write-Host "Docker compose error: $LASTEXITCODE" -ForegroundColor Red
        Write-Host $upOutput -ForegroundColor Red
        Invoke-Rollback
    }
    Write-Host "   ✅ Services started"

    Write-Host ""
    Write-Host "🏥 Step 3: Running health checks..."
    Write-Host ""

    Start-Sleep -Seconds 5

    if (-not (Test-ServiceHealth "MongoDB" 27017)) { Invoke-Rollback }
    if (-not (Test-ServiceHealth "User Service" 8081)) { Invoke-Rollback }
    if (-not (Test-ServiceHealth "Product Service" 8082)) { Invoke-Rollback }
    if (-not (Test-ServiceHealth "Media Service" 8083)) { Invoke-Rollback }
    if (-not (Test-ServiceHealth "API Gateway" 8080)) { Invoke-Rollback }
    if (-not (Test-ServiceHealth "Frontend HTTP" 4200)) { Invoke-Rollback }
    if (-not (Test-ServiceHealth "Frontend HTTPS" 4443)) { Invoke-Rollback }

    Write-Host ""
    Write-Host "==========================================="
    Write-Host "✅ Deployment Successful!" -ForegroundColor Green
    Write-Host "==========================================="

    Write-Host ""
    Write-Host "🌐 Application URLs:"
    Write-Host "   - https://localhost:4443"
    Write-Host "   - http://localhost:4200"
    Write-Host "   - http://localhost:8080"

    Write-Host ""
    Write-Host "📊 Docker status:"
    docker compose ps

    exit 0
}
catch {
    Write-Host ""
    Write-Host "Error during deployment: $($_.Exception.Message)" -ForegroundColor Red
    Invoke-Rollback
}
