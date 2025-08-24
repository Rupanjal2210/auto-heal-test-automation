#!/bin/bash

# Multi-Platform Auto-Healing Framework Launcher
# This script helps launch the framework with different configurations

echo "======================================"
echo "Multi-Platform Auto-Healing Framework"
echo "======================================"

# Default values
MODE="demo"
CONFIG_FILE="src/main/resources/healing-config.yml"
PLATFORM="all"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -m|--mode)
            MODE="$2"
            shift 2
            ;;
        -c|--config)
            CONFIG_FILE="$2"
            shift 2
            ;;
        -p|--platform)
            PLATFORM="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -m, --mode MODE        Run mode: demo, test, server (default: demo)"
            echo "  -c, --config FILE      Configuration file path (default: src/main/resources/healing-config.yml)"
            echo "  -p, --platform TYPE    Platform to test: web, windows, applet, mainframe, all (default: all)"
            echo "  -h, --help             Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 --mode demo --platform web"
            echo "  $0 --mode test --config custom-config.yml"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use -h or --help for usage information"
            exit 1
            ;;
    esac
done

echo "Mode: $MODE"
echo "Config: $CONFIG_FILE"
echo "Platform: $PLATFORM"
echo ""

# Build the project if needed
if [ ! -d "target" ]; then
    echo "Building project..."
    mvn clean compile
fi

# Set Java classpath
CLASSPATH="target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q)"

# Launch based on mode
case $MODE in
    "demo")
        echo "Running demo mode..."
        java -cp "$CLASSPATH" com.autohealing.AutoHealingFramework demo
        ;;
    "test")
        echo "Running tests..."
        mvn test
        ;;
    "server")
        echo "Starting as server (not implemented yet)..."
        echo "Server mode will be available in future versions"
        ;;
    *)
        echo "Unknown mode: $MODE"
        echo "Available modes: demo, test, server"
        exit 1
        ;;
esac

echo ""
echo "Framework execution completed."
echo "Check the 'reports' directory for healing reports."
