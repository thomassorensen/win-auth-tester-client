#!/bin/bash
# Windows Authentication Tester - Launch Script with Bundled JRE

echo "================================================================================"
echo "Windows Authentication Tester Client (with bundled JRE)"
echo "================================================================================"
echo ""

# Determine the script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Check if bundled JRE exists
if [ -f "$SCRIPT_DIR/jre/bin/java" ]; then
    JAVA_CMD="$SCRIPT_DIR/jre/bin/java"
    echo "Using bundled JRE"
elif [ -f "$SCRIPT_DIR/jre/bin/java.exe" ]; then
    JAVA_CMD="$SCRIPT_DIR/jre/bin/java.exe"
    echo "Using bundled JRE"
else
    JAVA_CMD="java"
    echo "Using system Java"
fi

if [ -z "$1" ]; then
    echo "Usage: ./run-with-jre.sh [URL] [OPTIONS]"
    echo ""
    echo "Example:"
    echo "  ./run-with-jre.sh http://server:8080/protected-resource"
    echo "  ./run-with-jre.sh http://server:8080/protected-resource -n"
    echo "  ./run-with-jre.sh http://server:8080/protected-resource -U username -P password -d DOMAIN"
    echo ""
    echo "Run './run-with-jre.sh --help' for all options"
    exit 1
fi

if [ ! -f "$SCRIPT_DIR/win-auth-tester-client-1.0.0-jar-with-dependencies.jar" ]; then
    echo "ERROR: Application JAR not found in current directory"
    exit 1
fi

"$JAVA_CMD" -jar "$SCRIPT_DIR/win-auth-tester-client-1.0.0-jar-with-dependencies.jar" -u "$@"
