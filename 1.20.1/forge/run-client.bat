@echo off
setlocal
cd /d "%~dp0"

if defined JAVA17_HOME (
    set "JAVA_HOME=%JAVA17_HOME%"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17"
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-17"
)

if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [Vein Farming Forge 1.20.1] Launching Minecraft Client with Java: %JAVA_HOME%...
call gradlew.bat runClient --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Game launch failed!
    pause
    exit /b %ERRORLEVEL%
)
pause
