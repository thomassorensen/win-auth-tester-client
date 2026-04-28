# Building Release Distributions

This document explains how to build different types of release distributions for the Windows Authentication Tester Client.

## Prerequisites

- JDK 11 or higher installed
- Maven 3.6+ installed
- Windows operating system (for testing the release)

## Standard Build (No JRE)

Build the standard JAR files without bundled JRE:

```bash
mvn clean package
```

**Output:**
- `target/win-auth-tester-client-1.0.0.jar` - Standard JAR
- `target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar` - Standalone JAR

**Usage:**
Requires Java 11+ to be installed on the target system.

```bash
java -jar win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u <URL>
```

## Release with Bundled JRE

Build a distribution package with a bundled Java Runtime Environment (JRE). This allows the application to run on systems without Java installed.

### Method 1: Using Build Script (Recommended)

The easiest way to create a release with bundled JRE:

**Windows:**
```batch
create-release-with-jre.bat
```

**Linux/macOS/Git Bash:**
```bash
./create-release-with-jre.sh
```

**Requirements:**
- JDK 11+ installed
- JAVA_HOME environment variable set
- Maven installed

**Output:**
- `target/win-auth-tester-client-1.0.0-windows-x64-jre.zip`

### Method 2: Using Maven Profile (Advanced)

Alternative method using Maven profile:

```bash
mvn clean package -P release-with-jre
```

**Note:** This method requires the moditect Maven plugin and may need additional configuration depending on your environment.

### Build Command

```bash
mvn clean package -P release-with-jre
```

### What Gets Created

**Output:**
- `target/win-auth-tester-client-1.0.0-windows-x64.zip`

**ZIP Contents:**
```
win-auth-tester-client-1.0.0/
├── jre/                                          # Bundled Java Runtime
│   ├── bin/
│   │   ├── java.exe
│   │   └── ... (other Java executables)
│   ├── lib/
│   └── ... (other JRE components)
├── lib/                                          # Application dependencies
│   ├── win-auth-tester-client-1.0.0.jar
│   ├── httpclient-4.5.14.jar
│   ├── httpclient-win-4.5.14.jar
│   └── ... (all other dependencies)
├── win-auth-tester-client-1.0.0-jar-with-dependencies.jar  # Standalone JAR
├── run.bat                                       # Standard launcher (requires Java)
├── run.sh                                        # Standard launcher (bash)
├── run-with-jre.bat                             # Launcher using bundled JRE
├── run-with-jre.sh                              # Launcher using bundled JRE (bash)
├── README.md                                     # Full documentation
├── QUICKREF.md                                   # Quick reference
└── EXAMPLES.md                                   # Usage examples
```

### JRE Details

The bundled JRE includes only the modules required by the application:
- `java.base` - Core Java functionality
- `java.logging` - Logging support
- `java.xml` - XML processing
- `java.naming` - JNDI/naming services
- `java.management` - JMX management
- `java.sql` - JDBC support
- `jdk.crypto.ec` - Elliptic curve cryptography
- `jdk.localedata` - Locale data
- `jdk.unsupported` - Required by some dependencies

**Size Optimization:**
- Debug symbols stripped
- Man pages and headers removed
- Compressed (level 2)
- Typical size: 40-60 MB (vs 200+ MB for full JDK)

## Deployment

### With Bundled JRE (Recommended for Distribution)

1. Build the release:
   ```bash
   mvn clean package -P release-with-jre
   ```

2. Extract the ZIP on target system:
   ```bash
   unzip win-auth-tester-client-1.0.0-windows-x64.zip
   cd win-auth-tester-client-1.0.0
   ```

3. Run using bundled JRE:
   ```batch
   run-with-jre.bat http://server:8080/api
   ```

**Advantages:**
- ✅ No Java installation required on target system
- ✅ Guaranteed JRE version compatibility
- ✅ Self-contained distribution
- ✅ Easier deployment in restricted environments

### Without Bundled JRE (Standard Distribution)

1. Build standard:
   ```bash
   mvn clean package
   ```

2. Copy JAR to target system:
   ```bash
   scp target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar user@server:/path/
   ```

3. Run (requires Java 11+ on target):
   ```bash
   java -jar win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u <URL>
   ```

## Build Profiles

### Default Profile
Standard Maven build without JRE bundling.

```bash
mvn clean package
```

### release-with-jre Profile
Creates a distribution with bundled JRE.

```bash
mvn clean package -P release-with-jre
```

**Profile Actions:**
1. Copies all dependencies to `target/release/lib/`
2. Copies application JAR to release directory
3. Creates custom JRE using jlink/moditect
4. Copies documentation and scripts
5. Creates ZIP archive with everything

## Customizing the Bundled JRE

To modify which Java modules are included, edit `pom.xml`:

```xml
<configuration>
    <modules>
        <module>java.base</module>
        <module>java.logging</module>
        <!-- Add more modules here -->
    </modules>
</configuration>
```

To find required modules, run:
```bash
jdeps -s target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar
```

## Platform-Specific Builds

The current configuration creates a Windows x64 distribution. To create distributions for other platforms:

1. Run the build on the target platform (Linux, macOS)
2. The bundled JRE will match the build platform
3. Update the assembly descriptor finalName for the platform

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build Release

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '11'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean package -P release-with-jre
      - name: Upload Release
        uses: actions/upload-artifact@v3
        with:
          name: win-auth-tester-windows-x64
          path: target/*.zip
```

### Jenkins Example

```groovy
pipeline {
    agent any
    tools {
        maven 'Maven 3.8'
        jdk 'JDK 11'
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package -P release-with-jre'
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'target/*.zip', fingerprint: true
            }
        }
    }
}
```

## Troubleshooting Build Issues

### moditect Plugin Issues

If the moditect plugin fails, you may need to add the JDK's jmods directory to the module path. Update the plugin configuration:

```xml
<modulePath>
    <path>${java.home}/jmods</path>
    <path>${project.build.directory}/release/lib</path>
</modulePath>
```

### Missing Modules

If you get "module not found" errors at runtime, analyze dependencies:

```bash
jdeps --list-deps target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar
```

Add missing modules to the `<modules>` section in pom.xml.

### Large JRE Size

If the bundled JRE is too large:
1. Remove unnecessary modules
2. Increase compression level (0-2)
3. Use `stripDebug` option

## Version Updates

When updating the version:

1. Update `pom.xml` version
2. Update JAR name in `run-with-jre.bat`
3. Update JAR name in `run-with-jre.sh`
4. Update documentation

## Testing the Release

After building:

1. Extract the ZIP
2. Navigate to the directory
3. Run without system Java:
   ```batch
   run-with-jre.bat http://httpbin.org/status/401
   ```
4. Verify it works without Java in PATH

## Best Practices

1. **Always test on clean system** - Verify JRE bundle works without system Java
2. **Document JRE version** - Include in release notes
3. **Sign executables** - Consider code signing for production
4. **Virus scan** - Scan bundle before distribution
5. **Test all platforms** - If supporting multiple OS

## File Sizes

Approximate sizes for reference:
- Standard JAR: ~500 KB
- JAR with dependencies: ~3.5 MB
- Release with JRE (ZIP): ~45-60 MB (compressed)
- Extracted release: ~120-150 MB

## Support

For build issues:
1. Check Maven output for errors
2. Verify JDK 11+ is being used
3. Ensure Maven 3.6+ is installed
4. Review moditect plugin documentation
5. Check that all dependencies are compatible with Java modules
