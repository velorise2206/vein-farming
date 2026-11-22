@echo off
setlocal
set "REPO_ROOT=%~dp0.."
cd /d "%REPO_ROOT%"

set "CHOICE=%~1"
if not "%CHOICE%"=="" goto :HANDLE_CHOICE

:MENU
cls
echo ====================================================
echo          Vein Farming - Forge Dev Client
echo ====================================================
echo   1) Forge 1.20.1    (Java 17)
echo   2) Forge 1.21.1    (Java 21)
echo   3) Forge 1.21.11   (Java 21)
echo   4) Forge 26.1.2    (Java 25)
echo   5) Forge 26.2      (Java 25)
echo   6) Forge 26.3      (Java 25)
echo   0) Exit
echo ----------------------------------------------------
set /p "CHOICE=Select version to run [1-6, default=1]: "
if "%CHOICE%"=="" set "CHOICE=1"

:HANDLE_CHOICE
if "%CHOICE%"=="1" goto :RUN_1201
if "%CHOICE%"=="1.20.1" goto :RUN_1201
if "%CHOICE%"=="1201" goto :RUN_1201

if "%CHOICE%"=="2" goto :RUN_1211
if "%CHOICE%"=="1.21.1" goto :RUN_1211
if "%CHOICE%"=="1211" goto :RUN_1211

if "%CHOICE%"=="3" goto :RUN_12111
if "%CHOICE%"=="1.21.11" goto :RUN_12111
if "%CHOICE%"=="12111" goto :RUN_12111

if "%CHOICE%"=="4" goto :RUN_261
if "%CHOICE%"=="26.1" goto :RUN_261
if "%CHOICE%"=="26.1.2" goto :RUN_261
if "%CHOICE%"=="261" goto :RUN_261

if "%CHOICE%"=="5" goto :RUN_262
if "%CHOICE%"=="26.2" goto :RUN_262
if "%CHOICE%"=="262" goto :RUN_262

if "%CHOICE%"=="6" goto :RUN_263
if "%CHOICE%"=="26.3" goto :RUN_263
if "%CHOICE%"=="263" goto :RUN_263

if "%CHOICE%"=="0" exit /b 0

echo Invalid selection: "%CHOICE%"
timeout /t 2 >nul
goto :MENU

:RUN_1201
set "TARGET_DIR=1.20.1\forge"
call :SETUP_JAVA17
goto :START_CLIENT

:RUN_1211
set "TARGET_DIR=1.21.1\forge"
call :SETUP_JAVA21
goto :START_CLIENT

:RUN_12111
set "TARGET_DIR=1.21.11\forge"
call :SETUP_JAVA21
goto :START_CLIENT

:RUN_261
set "TARGET_DIR=26.1\forge"
call :SETUP_JAVA25
goto :START_CLIENT

:RUN_262
set "TARGET_DIR=26.2\forge"
call :SETUP_JAVA25
goto :START_CLIENT

:RUN_263
set "TARGET_DIR=26.3\forge"
call :SETUP_JAVA25
goto :START_CLIENT

:SETUP_JAVA17
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
exit /b 0

:SETUP_JAVA21
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
exit /b 0

:SETUP_JAVA25
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
exit /b 0

:START_CLIENT
if exist "C:\Program Files\Eclipse Adoptium\jdk-8.0.512.9-hotspot\bin\java.exe" (
    set "JDK8=C:\Program Files\Eclipse Adoptium\jdk-8.0.512.9-hotspot"
)
echo [Vein Farming] Launching %TARGET_DIR% Client with Java: %JAVA_HOME%...
cd /d "%REPO_ROOT%\%TARGET_DIR%"
call gradlew.bat runClient
set "EXITCODE=%ERRORLEVEL%"
cd /d "%REPO_ROOT%"
if not "%EXITCODE%"=="0" (
    echo.
    echo [Vein Farming] Forge client exited with error code %EXITCODE%.
    pause
)
exit /b %EXITCODE%
