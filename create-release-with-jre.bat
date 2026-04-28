@echo off
REM Script to create a release package with bundled JRE
REM This script creates a distribution that doesn't require Java to be installed

echo ================================================================================
echo Creating Windows Authentication Tester Release with Bundled JRE
echo ================================================================================
echo.

REM Check if Maven build has been done
if not exist "target\win-auth-tester-client-1.0.0-jar-with-dependencies.jar" (
    echo Building application first...
    call mvn clean package
    if errorlevel 1 (
        echo ERROR: Maven build failed
        exit /b 1
    )
)

REM Check if JAVA_HOME is set
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME environment variable is not set
    echo Please set JAVA_HOME to your JDK installation directory
    exit /b 1
)

REM Check if jlink is available
if not exist "%JAVA_HOME%\bin\jlink.exe" (
    echo ERROR: jlink not found in JAVA_HOME\bin
    echo Make sure you're using JDK 11 or higher
    exit /b 1
)

echo Creating release directory...
if exist "target\release-dist" rmdir /s /q "target\release-dist"
mkdir "target\release-dist"

echo.
echo Creating custom JRE with jlink...
"%JAVA_HOME%\bin\jlink.exe" ^
    --add-modules java.base,java.logging,java.xml,java.naming,java.management,java.sql,jdk.crypto.ec,jdk.localedata,jdk.unsupported ^
    --output "target\release-dist\jre" ^
    --strip-debug ^
    --no-header-files ^
    --no-man-pages ^
    --compress=2

if errorlevel 1 (
    echo ERROR: jlink failed
    exit /b 1
)

echo.
echo Copying application files...
copy "target\win-auth-tester-client-1.0.0-jar-with-dependencies.jar" "target\release-dist\" >nul
copy "run.bat" "target\release-dist\" >nul
copy "run.sh" "target\release-dist\" >nul
copy "run-with-jre.bat" "target\release-dist\" >nul
copy "run-with-jre.sh" "target\release-dist\" >nul
copy "README.md" "target\release-dist\" >nul
copy "QUICKREF.md" "target\release-dist\" >nul
copy "EXAMPLES.md" "target\release-dist\" >nul

echo.
echo Creating ZIP archive...
powershell -Command "Compress-Archive -Path 'target\release-dist\*' -DestinationPath 'target\win-auth-tester-client-1.0.0-windows-x64-jre.zip' -Force"

if errorlevel 1 (
    echo ERROR: Failed to create ZIP archive
    exit /b 1
)

echo.
echo ================================================================================
echo SUCCESS!
echo ================================================================================
echo.
echo Release package created: target\win-auth-tester-client-1.0.0-windows-x64-jre.zip
echo.

for %%F in ("target\win-auth-tester-client-1.0.0-windows-x64-jre.zip") do (
    echo Size: %%~zF bytes
)

echo.
echo To deploy:
echo   1. Extract the ZIP on target system
echo   2. Run: run-with-jre.bat [URL]
echo.
echo No Java installation required on target system!
echo ================================================================================
