@echo off
setlocal
cd /d "%~dp0"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo =====================================================================
echo                VEIN FARMING - CLIENT LAUNCHER
echo =====================================================================
echo   1) NeoForge 1.21.1  (Java 21)
echo   2) Fabric 1.21.1    (Java 21)
echo   3) Fabric 1.21.11   (Java 21)
echo   4) Forge 1.20.1     (Java 17)
echo   5) Fabric 1.20.1    (Java 17)
echo   6) NeoForge 26.1.2  (Java 25)
echo   7) Fabric 26.1.2    (Java 25)
echo   8) NeoForge 26.2    (Java 25)
echo   9) Fabric 26.2      (Java 25)
echo  10) NeoForge 26.3    (Java 25)
echo  11) Fabric 26.3      (Java 25)
echo  12) NeoForge 1.21.11 (Java 21)
echo.
echo   0) Exit
echo ---------------------------------------------------------------------
set /p "CHOICE=Select a loader/version [1-12]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" (
    call "%~dp0scripts\run-neoforge.bat" 1.21.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="2" (
    call "%~dp0scripts\run-fabric.bat" 1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="3" (
    call "%~dp0scripts\run-fabric.bat" 2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="4" (
    call "%~dp0scripts\run-forge-1.20.1.bat"
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="5" (
    call "%~dp0scripts\run-fabric.bat" 3
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="6" (
    call "%~dp0scripts\run-neoforge.bat" 26.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="7" (
    call "%~dp0scripts\run-fabric.bat" 4
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="8" (
    call "%~dp0scripts\run-neoforge.bat" 26.2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="9" (
    call "%~dp0scripts\run-fabric.bat" 5
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="10" (
    call "%~dp0scripts\run-neoforge.bat" 26.3
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="11" (
    call "%~dp0scripts\run-fabric.bat" 6
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="12" (
    call "%~dp0scripts\run-neoforge.bat" 1.21.11
    exit /b %ERRORLEVEL%
)

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU
