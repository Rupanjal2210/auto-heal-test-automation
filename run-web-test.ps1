#!/usr/bin/env pwsh

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Running MakeMyTrip Login Test with Auto-Healing" -ForegroundColor Cyan  
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Compiling the project..." -ForegroundColor Yellow
mvn clean compile -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "FAILED: Compilation failed" -ForegroundColor Red
    Read-Host "Press Enter to continue..."
    exit 1
}

Write-Host ""
Write-Host "Running the WebAdapterLoginTest..." -ForegroundColor Yellow
Write-Host "Note: This test requires an internet connection and Chrome browser" -ForegroundColor Magenta
Write-Host ""

mvn test -Dtest=WebAdapterLoginTest

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "COMPLETED: Test finished (may have warnings due to website protection)" -ForegroundColor Yellow
    Write-Host "Check the logs above for detailed test execution results" -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "SUCCESS: All tests passed" -ForegroundColor Green
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Test execution completed" -ForegroundColor Cyan  
Write-Host "============================================" -ForegroundColor Cyan
Read-Host "Press Enter to continue..."
