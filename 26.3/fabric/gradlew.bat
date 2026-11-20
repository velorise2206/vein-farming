@echo off
setlocal EnableExtensions
set "GRADLE_BAT=%USERPROFILE%\.gradle\wrapper\dists\gradle-9.8.1-bin\a2l6ltp3k1vth6lwipzm5p2jp\gradle-9.8.1\bin\gradle.bat"
if not exist "%GRADLE_BAT%" (
    set "GRADLE_VERSION=9.8.1"
    set "GRADLE_DIST=gradle-%GRADLE_VERSION%-bin.zip"
    set "GRADLE_URL=https://services.gradle.org/distributions/%GRADLE_DIST%"
    set "GRADLE_CACHE=%USERPROFILE%\.gradle\camera-lockon-distributions"
    set "GRADLE_HOME=%GRADLE_CACHE%\gradle-%GRADLE_VERSION%"
    set "GRADLE_BAT=%GRADLE_HOME%\bin\gradle.bat"
)
call "%GRADLE_BAT%" %*
exit /b %ERRORLEVEL%
