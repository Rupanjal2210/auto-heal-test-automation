@echo off
echo ============================================
echo Running WebAdapter Login Test with Healing Reports
echo ============================================
echo.

echo Compiling the project...
mvn clean compile -q

if %ERRORLEVEL% NEQ 0 (
    echo FAILED: Compilation failed
    pause
    exit /b 1
)

echo.
echo Running the WebAdapterLoginTest with detailed reporting...
echo Note: This test will generate healing reports and screenshots
echo.

mvn test -Dtest=WebAdapterLoginTest

echo.
echo ============================================
echo Test execution completed
echo ============================================
echo.

echo Opening generated reports...
echo.

REM Find the most recent HTML report
for /f "delims=" %%i in ('dir "test-reports\healing-reports\*.html" /b /o-d 2^>nul') do (
    set "latest_report=%%i"
    goto :found
)

:found
if defined latest_report (
    echo Opening latest healing report: %latest_report%
    start "" "test-reports\healing-reports\%latest_report%"
) else (
    echo No HTML reports found in test-reports\healing-reports\
)

echo.
echo Check the following directories for reports:
echo   - test-reports\healing-reports\ (HTML and JSON reports)
echo   - test-reports\screenshots\ (Test screenshots)
echo.

pause
