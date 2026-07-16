@echo off
setlocal enabledelayedexpansion

REM ================================================================
REM  scan-mainframe.bat
REM  Mainframe Veracode Vulnerability Scanner — Batch Entry Point
REM
REM  Usage : scan-mainframe.bat [target_directory]
REM  Default: scans current working directory recursively
REM
REM  Output : .github\output\mainframe-scan-YYYYMMDD.csv
REM  Engine : scan-mainframe-core.ps1 (same directory as this script)
REM ================================================================

set "SCRIPT_DIR=%~dp0"
set "TARGET_DIR=%~1"
if "%TARGET_DIR%"=="" set "TARGET_DIR=."

REM Resolve output directory relative to workspace root (parent of .github\scripts\)
set "OUTPUT_DIR=%SCRIPT_DIR%..\..\output"
if not exist "%OUTPUT_DIR%" (
    mkdir "%OUTPUT_DIR%"
    if !ERRORLEVEL! NEQ 0 (
        echo [ERROR] Could not create output directory: %OUTPUT_DIR%
        exit /b 1
    )
)

REM Build timestamped output filename using WMIC
for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value 2^>nul') do (
    set "DATETIME=%%I"
)
set "DATESTAMP=%DATETIME:~0,8%"
set "OUTFILE=%OUTPUT_DIR%\mainframe-scan-%DATESTAMP%.csv"

REM Confirm PowerShell is available
where powershell >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] PowerShell is not available on PATH. Cannot run scan engine.
    exit /b 1
)

REM Confirm the core scan script exists
if not exist "%SCRIPT_DIR%scan-mainframe-core.ps1" (
    echo [ERROR] scan-mainframe-core.ps1 not found at: %SCRIPT_DIR%
    echo [ERROR] Ensure both scan-mainframe.bat and scan-mainframe-core.ps1 are in the same directory.
    exit /b 1
)

echo [SCAN] ================================================================
echo [SCAN]  Mainframe Veracode Scanner
echo [SCAN] ================================================================
echo [SCAN] Target directory : %TARGET_DIR%
echo [SCAN] Output file      : %OUTFILE%
echo [SCAN] Scan engine      : %SCRIPT_DIR%scan-mainframe-core.ps1
echo [SCAN] ----------------------------------------------------------------

REM Execute the PowerShell scan engine
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%scan-mainframe-core.ps1" ^
    -TargetDir "%TARGET_DIR%" ^
    -OutFile "%OUTFILE%"

set "PS_EXIT=%ERRORLEVEL%"

if %PS_EXIT% EQU 0 (
    echo [SCAN] ----------------------------------------------------------------
    echo [SCAN] Scan completed successfully.
    echo [SCAN] Results saved to: %OUTFILE%
    echo [SCAN] ================================================================
) else (
    echo [SCAN] ----------------------------------------------------------------
    echo [ERROR] Scan engine exited with code %PS_EXIT%.
    echo [ERROR] Check PowerShell error output above for details.
    exit /b %PS_EXIT%
)

endlocal
exit /b 0
