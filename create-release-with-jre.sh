#!/bin/bash
# Script to create a release package with bundled JRE
# This script creates a distribution that doesn't require Java to be installed

echo "================================================================================"
echo "Creating Windows Authentication Tester Release with Bundled JRE"
echo "================================================================================"
echo ""

# Check if Maven build has been done
if [ ! -f "target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar" ]; then
    echo "Building application first..."
    mvn clean package
    if [ $? -ne 0 ]; then
        echo "ERROR: Maven build failed"
        exit 1
    fi
fi

# Check if JAVA_HOME is set
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME environment variable is not set"
    echo "Please set JAVA_HOME to your JDK installation directory"
    exit 1
fi

# Check if jlink is available
if [ ! -f "$JAVA_HOME/bin/jlink" ]; then
    echo "ERROR: jlink not found in JAVA_HOME/bin"
    echo "Make sure you're using JDK 11 or higher"
    exit 1
fi

echo "Creating release directory..."
rm -rf "target/release-dist"
mkdir -p "target/release-dist"

echo ""
echo "Creating custom JRE with jlink..."
"$JAVA_HOME/bin/jlink" \
    --add-modules java.base,java.logging,java.xml,java.naming,java.management,java.sql,jdk.crypto.ec,jdk.localedata,jdk.unsupported \
    --output "target/release-dist/jre" \
    --strip-debug \
    --no-header-files \
    --no-man-pages \
    --compress=2

if [ $? -ne 0 ]; then
    echo "ERROR: jlink failed"
    exit 1
fi

echo ""
echo "Copying application files..."
cp "target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar" "target/release-dist/"
cp "run.bat" "target/release-dist/"
cp "run.sh" "target/release-dist/"
cp "run-with-jre.bat" "target/release-dist/"
cp "run-with-jre.sh" "target/release-dist/"
chmod +x "target/release-dist/run.sh"
chmod +x "target/release-dist/run-with-jre.sh"
cp "README.md" "target/release-dist/"
cp "QUICKREF.md" "target/release-dist/"
cp "EXAMPLES.md" "target/release-dist/"

echo ""
echo "Creating ZIP archive..."
cd target
zip -r -q "win-auth-tester-client-1.0.0-windows-x64-jre.zip" release-dist/
cd ..

if [ $? -ne 0 ]; then
    echo "ERROR: Failed to create ZIP archive"
    exit 1
fi

echo ""
echo "================================================================================"
echo "SUCCESS!"
echo "================================================================================"
echo ""
echo "Release package created: target/win-auth-tester-client-1.0.0-windows-x64-jre.zip"
echo ""

SIZE=$(du -h "target/win-auth-tester-client-1.0.0-windows-x64-jre.zip" | cut -f1)
echo "Size: $SIZE"

echo ""
echo "To deploy:"
echo "  1. Extract the ZIP on target system"
echo "  2. Run: ./run-with-jre.sh [URL]"
echo ""
echo "No Java installation required on target system!"
echo "================================================================================"
