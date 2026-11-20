@echo off
setlocal
cd /d "%~dp0"

if defined JAVA25_HOME (
    set "JAVA_HOME=%JAVA25_HOME%"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.5.8-hotspot"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-25\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25"
) else if exist "C:\Program Files\Java\jdk-25\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25"
)

if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [Vein Farming Fabric 26.1.2] Launching Minecraft Client with Java: %JAVA_HOME%...
call gradlew.bat runClient --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Game launch failed!
    pause
    exit /b %ERRORLEVEL%
)
pause
