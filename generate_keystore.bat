@echo off
echo ============================================
echo   NuengTranslator - Generate Release Keystore
echo ============================================
echo.

mkdir app\keystore 2>nul

keytool -genkeypair -v ^
    -keystore app\keystore\nueng-translator.jks ^
    -keyalg RSA ^
    -keysize 2048 ^
    -validity 10000 ^
    -alias nuengtranslator ^
    -storepass NuengTranslator2026 ^
    -keypass NuengTranslator2026 ^
    -dname "CN=NuengTranslator, OU=Dev, O=Nueng, L=Unknown, ST=Unknown, C=TH"

echo.
if exist app\keystore\nueng-translator.jks (
    echo [SUCCESS] Keystore created at app\keystore\nueng-translator.jks
) else (
    echo [ERROR] Keystore generation failed!
    echo Make sure keytool is in your PATH (comes with Java/Android Studio)
)
echo.
pause
