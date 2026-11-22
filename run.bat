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
echo   1) Forge 1.20.1     (Java 17)
echo   2) Fabric 1.20.1    (Java 17)
echo   3) Forge 1.21.1     (Java 21)
echo   4) NeoForge 1.21.1  (Java 21)
echo   5) Fabric 1.21.1    (Java 21)
echo   6) Forge 1.21.11    (Java 21)
echo   7) NeoForge 1.21.11 (Java 21)
echo   8) Fabric 1.21.11   (Java 21)
echo   9) Forge 26.1.2     (Java 25)
echo  10) NeoForge 26.1.2  (Java 25)
echo  11) Fabric 26.1.2    (Java 25)
echo  12) Forge 26.2       (Java 25)
echo  13) NeoForge 26.2    (Java 25)
echo  14) Fabric 26.2      (Java 25)
echo  15) Forge 26.3       (Java 25)
echo  16) NeoForge 26.3    (Java 25)
echo  17) Fabric 26.3      (Java 25)
echo.
echo   0) Exit
echo ---------------------------------------------------------------------
set /p "CHOICE=Select a loader/version [1-17]: "

:HANDLE_CHOICE
if "%CHOICE%"=="1" (
    call "%~dp0scripts\run-forge-1.20.1.bat" 1.20.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="2" (
    call "%~dp0scripts\run-fabric.bat" 1.20.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="3" (
    call "%~dp0scripts\run-forge-1.20.1.bat" 1.21.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="4" (
    call "%~dp0scripts\run-neoforge.bat" 1.21.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="5" (
    call "%~dp0scripts\run-fabric.bat" 1.21.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="6" (
    call "%~dp0scripts\run-forge-1.20.1.bat" 1.21.11
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="7" (
    call "%~dp0scripts\run-neoforge.bat" 1.21.11
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="8" (
    call "%~dp0scripts\run-fabric.bat" 1.21.11
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="9" (
    call "%~dp0scripts\run-forge-1.20.1.bat" 26.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="10" (
    call "%~dp0scripts\run-neoforge.bat" 26.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="11" (
    call "%~dp0scripts\run-fabric.bat" 26.1
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="12" (
    call "%~dp0scripts\run-forge-1.20.1.bat" 26.2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="13" (
    call "%~dp0scripts\run-neoforge.bat" 26.2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="14" (
    call "%~dp0scripts\run-fabric.bat" 26.2
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="15" (
    call "%~dp0scripts\run-forge-1.20.1.bat" 26.3
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="16" (
    call "%~dp0scripts\run-neoforge.bat" 26.3
    exit /b %ERRORLEVEL%
)
if "%CHOICE%"=="17" (
    call "%~dp0scripts\run-fabric.bat" 26.3
    exit /b %ERRORLEVEL%
)

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU
