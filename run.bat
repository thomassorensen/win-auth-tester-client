@echo off
REM Windows Authentication Tester - Launch Script

echo ================================================================================
echo Windows Authentication Tester Client
echo ================================================================================
echo.

if "%1"=="" (
    echo Usage: run.bat [URL] [OPTIONS]
    echo.
    echo Example:
    echo   run.bat http://server:8080/protected-resource
    echo   run.bat http://server:8080/protected-resource -n
    echo   run.bat http://server:8080/protected-resource -U username -P password -d DOMAIN
    echo.
    echo Run 'java -jar target\win-auth-tester-client-1.0.0-jar-with-dependencies.jar --help' for all options
    exit /b 1
)

if not exist "target\win-auth-tester-client-1.0.0-jar-with-dependencies.jar" (
    echo ERROR: JAR file not found. Please run 'mvn clean package' first.
    exit /b 1
)

java -jar target\win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u %*
