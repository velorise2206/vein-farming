@echo off
setlocal
cd /d "%~dp0"

if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21"
) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
)

if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [Vein Farming Fabric 1.21.11] Launching Minecraft Client with Java: %JAVA_HOME%...
call gradlew.bat runClient --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Game launch failed!
    pause
    exit /b %ERRORLEVEL%
)
pause
