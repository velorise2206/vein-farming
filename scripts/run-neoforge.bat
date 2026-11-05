@echo off
setlocal
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

if defined JAVA21_HOME set "JAVA_HOME=%JAVA21_HOME%"

echo [Vein Farming] Launching NeoForge 1.21.1 Client...
cd /d "%REPO_ROOT%\neoforge-1.21.1"
call gradlew.bat runClient
set "EXITCODE=%ERRORLEVEL%"
cd /d "%REPO_ROOT%"
if not "%EXITCODE%"=="0" (
    echo.
    echo [Vein Farming] NeoForge client exited with error code %EXITCODE%.
    pause
)
exit /b %EXITCODE%

