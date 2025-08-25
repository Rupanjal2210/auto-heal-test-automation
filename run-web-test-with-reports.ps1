#!/usr/bin/env pwsh

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Running WebAdapter Login Test with Healing Reports" -ForegroundColor Cyan  
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
Write-Host "Running the WebAdapterLoginTest with detailed reporting..." -ForegroundColor Yellow
Write-Host "Note: This test will generate healing reports and screenshots" -ForegroundColor Magenta
Write-Host ""

mvn test -Dtest=WebAdapterLoginTest

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Test execution completed" -ForegroundColor Cyan  
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Opening generated reports..." -ForegroundColor Yellow
Write-Host ""

# Find the most recent HTML report
$reportDir = "test-reports\healing-reports"
if (Test-Path $reportDir) {
    $latestReport = Get-ChildItem -Path $reportDir -Filter "*.html" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    
    if ($latestReport) {
        Write-Host "Opening latest healing report: $($latestReport.Name)" -ForegroundColor Green
        Start-Process $latestReport.FullName
    } else {
        Write-Host "No HTML reports found in $reportDir" -ForegroundColor Yellow
    }
} else {
    Write-Host "Report directory not found: $reportDir" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Check the following directories for reports:" -ForegroundColor Cyan
Write-Host "  - test-reports\healing-reports\ (HTML and JSON reports)" -ForegroundColor White
Write-Host "  - test-reports\screenshots\ (Test screenshots)" -ForegroundColor White
Write-Host ""

# List generated files
if (Test-Path "test-reports") {
    Write-Host "Generated files:" -ForegroundColor Cyan
    Get-ChildItem -Path "test-reports" -Recurse -File | ForEach-Object {
        Write-Host "  $($_.FullName)" -ForegroundColor Gray
    }
}

Write-Host ""
Read-Host "Press Enter to continue..."
