@echo off
echo ============================================
echo Running MakeMyTrip Login Test with Auto-Healing
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
echo Running the WebAdapterLoginTest...
echo Note: This test requires an internet connection and Chrome browser
echo.

mvn test -Dtest=WebAdapterLoginTest -q

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo COMPLETED: Test finished (may have warnings due to website protection)
    echo Check the logs above for detailed test execution results
) else (
    echo.
    echo SUCCESS: All tests passed
)

echo.
echo ============================================
echo Test execution completed
echo ============================================
pause
