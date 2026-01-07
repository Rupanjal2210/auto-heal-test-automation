@echo off
REM Multi-Platform Auto-Healing Framework Launcher for Windows
REM This batch script helps launch the framework with different configurations

echo ======================================
echo Multi-Platform Auto-Healing Framework
echo ======================================

REM Default values
set MODE=demo
set CONFIG_FILE=src/main/resources/healing-config.yml
set PLATFORM=all

REM Parse command line arguments
:parse_args
if "%1"=="" goto :run_framework
if "%1"=="-m" (
    set MODE=%2
    shift
    shift
    goto :parse_args
)
if "%1"=="--mode" (
    set MODE=%2
    shift
    shift
    goto :parse_args
)
if "%1"=="-c" (
    set CONFIG_FILE=%2
    shift
    shift
    goto :parse_args
)
if "%1"=="--config" (
    set CONFIG_FILE=%2
    shift
    shift
    goto :parse_args
)
if "%1"=="-p" (
    set PLATFORM=%2
    shift
    shift
    goto :parse_args
)
if "%1"=="--platform" (
    set PLATFORM=%2
    shift
    shift
    goto :parse_args
)
if "%1"=="-h" goto :show_help
if "%1"=="--help" goto :show_help

echo Unknown option: %1
echo Use -h or --help for usage information
exit /b 1

:show_help
echo Usage: %0 [OPTIONS]
echo.
echo Options:
echo   -m, --mode MODE        Run mode: demo, transparent, test, server (default: demo)
echo   -c, --config FILE      Configuration file path (default: src/main/resources/healing-config.yml)
echo   -p, --platform TYPE    Platform to test: web, windows, applet, mainframe, all (default: all)
echo   -h, --help             Show this help message
echo.
echo Examples:
echo   %0 --mode transparent
echo   %0 --mode demo --platform web
echo   %0 --mode test --config custom-config.yml
exit /b 0

:run_framework
echo Mode: %MODE%
echo Config: %CONFIG_FILE%
echo Platform: %PLATFORM%
echo.

REM Build the project if needed
if not exist "target" (
    echo Building project...
    mvn clean compile
    if errorlevel 1 (
        echo Build failed!
        exit /b 1
    )
)

REM Launch based on mode
if "%MODE%"=="demo" (
    echo Running demo mode...
    mvn exec:java -Dexec.mainClass="com.autohealing.AutoHealingFramework" -Dexec.args="demo"
) else if "%MODE%"=="transparent" (
    echo Running transparent healing demo...
    mvn exec:java -Dexec.mainClass="com.autohealing.demo.TransparentHealingDemo"
) else if "%MODE%"=="test" (
    echo Running tests...
    mvn test
) else if "%MODE%"=="server" (
    echo Starting as server (not implemented yet)...
    echo Server mode will be available in future versions
) else (
    echo Unknown mode: %MODE%
    echo Available modes: demo, transparent, test, server
    exit /b 1
)

echo.
echo Framework execution completed.
echo Check the 'reports' directory for healing reports.
echo.
pause
