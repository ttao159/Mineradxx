@echo off
chcp 65001 >nul
echo ========================================
echo  Mineradio Android APK Builder
echo ========================================

set PROJECT_DIR=%~dp0
set NODE_PROJECT=%PROJECT_DIR%app\src\main\assets\nodejs-project

echo [1/3] Installing Node.js backend dependencies...
cd /d "%NODE_PROJECT%"
call npm install
if %ERRORLEVEL% NEQ 0 (
    echo Warning: npm install failed, some music features may not work.
)

if not defined JAVA_HOME (
    echo.
    echo ERROR: JAVA_HOME is not set.
    echo Please install JDK 17 and set JAVA_HOME, for example:
    echo     set JAVA_HOME=C:\Program Files\Java\jdk-17
    echo.
    pause
    exit /b 1
)

echo [2/3] Building APK (JAVA_HOME=%JAVA_HOME%)...
cd /d "%PROJECT_DIR%"
call gradlew.bat assembleDebug

echo.
echo APK should be at: app\build\outputs\apk\debug\
pause
