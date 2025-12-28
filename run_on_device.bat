@echo off
setlocal enabledelayedexpansion
echo ===================================================
echo     TITAN SUITE: Deploy to Tablet (No Studio)
echo ===================================================
echo.
echo REQUIREMENTS:
echo 1. Connect Samsung Galaxy Tab A via USB.
echo 2. Enable "USB Debugging" in Developer Options.
echo.

REM --- Environment Setup (Mirrored from your WebTest config) ---
echo [1/4] Configuring environment...
set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot"
set "ANDROID_HOME=C:\Users\QuantumZer0\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

if not exist "%JAVA_HOME%" (
    echo [WARNING] JAVA_HOME not found. Using system default java.
) else (
    echo [OK] Java 21 Linked.
)

if not exist "%ANDROID_HOME%" (
    echo [ERROR] Android SDK not found at %ANDROID_HOME%
    pause
    exit /b
) else (
    echo [OK] Android SDK Linked.
)

REM --- Check for Device ---
echo.
echo [2/4] Checking for connected devices...
adb devices
echo.

REM --- Build Phase ---
echo [3/4] Compiling Titan Medical Suite (Gradle)...
call gradlew assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build Failed! Please check the logs above.
    pause
    exit /b
)

REM --- Deployment Phase ---
echo.
echo [4/4] Deploying APK to device...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Installation Failed!
    pause
    exit /b
)

echo.
echo [SUCCESS] App installed. Launching Main Activity...
adb shell am start -n com.example.androidapp/com.example.androidapp.MainActivity

echo.
echo ===================================================
echo     TITAN IS NOW LIVE ON YOUR TABLET! 🚀
echo ===================================================
pause
