@echo off
REM Windows Authentication Tester - Launch Script with Bundled JRE

echo ================================================================================
echo Windows Authentication Tester Client (with bundled JRE)
echo ================================================================================
echo.

REM Determine the script directory
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

REM Check if bundled JRE exists
if exist "%SCRIPT_DIR%jre\bin\java.exe" (
    set JAVA_CMD=%SCRIPT_DIR%jre\bin\java.exe
    echo Using bundled JRE
) else (
    set JAVA_CMD=java
    echo Using system Java
)

if "%1"=="" (
    echo Usage: run-with-jre.bat [URL] [OPTIONS]
    echo.
    echo Example:
    echo   run-with-jre.bat http://server:8080/protected-resource
    echo   run-with-jre.bat http://server:8080/protected-resource -n
    echo   run-with-jre.bat http://server:8080/protected-resource -U username -P password -d DOMAIN
    echo.
    echo Run 'run-with-jre.bat --help' for all options
    exit /b 1
)

if not exist "%SCRIPT_DIR%win-auth-tester-client-1.0.0-jar-with-dependencies.jar" (
    echo ERROR: Application JAR not found in current directory
    exit /b 1
)

"%JAVA_CMD%" -jar "%SCRIPT_DIR%win-auth-tester-client-1.0.0-jar-with-dependencies.jar" -u %*
