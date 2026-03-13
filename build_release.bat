@echo off
echo ============================================
echo   NuengTranslator - Build Release APK
echo ============================================
echo.

echo Step 1: Cleaning...
call gradlew.bat clean

echo.
echo Step 2: Building release APK...
call gradlew.bat assembleRelease

echo.
if exist app\build\outputs\apk\release\app-release.apk (
    echo ============================================
    echo   BUILD SUCCESS!
    echo ============================================
    echo.
    echo APK location:
    echo   app\build\outputs\apk\release\app-release.apk
    echo.
    echo Copy to your phone and install!
) else (
    echo [ERROR] Build failed! Check errors above.
)
echo.
pause
