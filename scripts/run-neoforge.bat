@echo off
setlocal
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21"
) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo [Vein Farming] Using Java 21 at: %JAVA_HOME%
)

echo [Vein Farming] Launching NeoForge 1.21.1 Client...
cd /d "%REPO_ROOT%\1.21.1\neoforge"
call gradlew.bat runClient
set "EXITCODE=%ERRORLEVEL%"
cd /d "%REPO_ROOT%"
if not "%EXITCODE%"=="0" (
    echo.
    echo [Vein Farming] NeoForge client exited with error code %EXITCODE%.
    pause
)
exit /b %EXITCODE%
