#!/bin/bash
# Windows Authentication Tester - Launch Script (For Windows running bash)

echo "================================================================================"
echo "Windows Authentication Tester Client"
echo "================================================================================"
echo ""

if [ -z "$1" ]; then
    echo "Usage: ./run.sh [URL] [OPTIONS]"
    echo ""
    echo "Example:"
    echo "  ./run.sh http://server:8080/protected-resource"
    echo "  ./run.sh http://server:8080/protected-resource -n"
    echo "  ./run.sh http://server:8080/protected-resource -U username -P password -d DOMAIN"
    echo ""
    echo "Run 'java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar --help' for all options"
    exit 1
fi

if [ ! -f "target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar" ]; then
    echo "ERROR: JAR file not found. Please run 'mvn clean package' first."
    exit 1
fi

java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u "$@"
