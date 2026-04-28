# Windows Authentication Tester Client

A comprehensive Java diagnostic tool for troubleshooting Windows authentication (NTLM/Kerberos) with Waffle-enabled servlets. This application provides verbose logging of the authentication handshake process, automatic problem detection, and actionable troubleshooting recommendations.

## Features

- 🔐 **Automatic Windows Authentication** - Uses the logged-in Windows user's credentials
- 🔍 **Verbose Logging** - Detailed logging of the entire authentication handshake process
- 🩺 **Diagnostic Capabilities** - Pre-connection checks for OS, network, DNS, and security settings
- 🛠️ **Troubleshooting Engine** - Intelligent analysis of failures with specific recommendations
- 📊 **Multiple Authentication Modes** - Support for NTLM, Negotiate (Kerberos), and explicit credentials
- 📝 **Comprehensive Logging** - Both console and file logging with detailed HTTP wire logs

## Requirements

- Java 11 or higher
- Windows operating system (required for Windows authentication)
- Maven 3.6+ (for building)
- Network access to the target servlet

## Building

### Standard Build

Build the application using Maven:

```bash
mvn clean package
```

This creates two JAR files in the `target` directory:
- `win-auth-tester-client-1.0.0.jar` - Standard JAR
- `win-auth-tester-client-1.0.0-jar-with-dependencies.jar` - Standalone JAR with all dependencies

### Release with Bundled JRE

To create a distribution with a bundled Java Runtime Environment (no Java installation required on target system):

**Windows:**
```batch
create-release-with-jre.bat
```

**Linux/macOS/Git Bash:**
```bash
./create-release-with-jre.sh
```

This creates:
- `target/win-auth-tester-client-1.0.0-windows-x64-jre.zip` - Complete distribution with bundled JRE (~45-60 MB)

The ZIP includes:
- Custom JRE (only required modules, ~40-50 MB)
- Application JAR with all dependencies
- Launch scripts (run-with-jre.bat/sh that use bundled JRE)
- Documentation

**Alternative:** Use Maven profile: `mvn clean package -P release-with-jre`

See [BUILD.md](BUILD.md) for detailed build instructions and customization options.

## Usage

### Basic Usage (Current Windows User)

Test authentication using the current logged-in Windows user:

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/protected-resource
```

### Using Explicit Credentials

Test with specific username and password:

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/protected-resource -U username -P password -d DOMAIN
```

### Force NTLM Authentication

Force NTLM authentication only (disable Negotiate/Kerberos):

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/protected-resource -n
```

### Force Negotiate Authentication

Force Negotiate authentication only (Kerberos with NTLM fallback):

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/protected-resource -g
```

### Custom Timeout

Set custom connection timeout (default: 30000ms):

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/protected-resource -t 60000
```

## Command Line Options

| Option | Long Option | Description | Required |
|--------|-------------|-------------|----------|
| `-u` | `--url` | Target servlet URL | Yes |
| `-n` | `--ntlm` | Force NTLM authentication only | No |
| `-g` | `--negotiate` | Force Negotiate authentication only | No |
| `-U` | `--username` | Username for authentication | No |
| `-P` | `--password` | Password for authentication | No |
| `-d` | `--domain` | Domain for authentication | No |
| `-t` | `--timeout` | Connection timeout in milliseconds | No |
| `-h` | `--help` | Display help message | No |

## Output

The application provides:

1. **Pre-Connection Diagnostics**
   - Operating system information
   - Current user and domain details
   - Network connectivity checks
   - DNS resolution
   - Windows security settings

2. **Authentication Process**
   - Detailed HTTP request/response logs
   - Authentication scheme negotiation
   - Header analysis
   - Response timing

3. **Results Summary**
   - Success/failure status
   - HTTP status code and reason
   - Server authentication methods
   - Response time

4. **Troubleshooting Analysis** (on failure)
   - Possible causes of failure
   - Recommended actions to resolve issues
   - Links to documentation

## Log Files

Logs are written to:
- **Console**: Real-time output with color coding
- **File**: `win-auth-tester.log` in the current directory with detailed information

## Common Issues and Solutions

### 401 Unauthorized - No WWW-Authenticate Header

**Cause**: Server not configured for Windows authentication

**Solution**:
- Verify Waffle filter is configured in web.xml
- Check Waffle JAR files are present
- Review servlet container logs

### 401 Unauthorized - Handshake Failed

**Cause**: Client credentials not accepted

**Solution**:
- Verify you're logged into a domain (not local account)
- Check user permissions and group membership
- Verify system time is synchronized
- Try explicit credentials

### Connection Timeout

**Cause**: Cannot reach the server

**Solution**:
- Verify server is running
- Check firewall rules
- Verify URL and port are correct
- Increase timeout with `-t` option

### DNS Resolution Failed

**Cause**: Hostname cannot be resolved

**Solution**:
- Verify URL is correct
- Check DNS server configuration
- Try using IP address instead

## Architecture

The application consists of several components:

- **WindowsAuthTester**: Main entry point and CLI argument handling
- **WindowsAuthClient**: HTTP client with Windows authentication support
- **DiagnosticRunner**: Pre-connection diagnostic checks
- **TroubleshootingEngine**: Failure analysis and recommendations
- **ResultPresenter**: User-friendly result display
- **AuthenticationResult**: Data model for authentication results

## Dependencies

Key dependencies used:

- **Apache HttpClient 4.5.14**: HTTP client with Windows authentication
- **Apache HttpClient Win**: Windows-specific authentication schemes
- **JNA 5.13.0**: Native Windows API access
- **SLF4J + Logback**: Logging framework
- **Apache Commons CLI**: Command line parsing

## Development

### Running Tests

```bash
mvn test
```

### Building Documentation

```bash
mvn javadoc:javadoc
```

### Code Style

The project follows standard Java conventions:
- Clear, descriptive variable names
- Comprehensive logging
- Proper exception handling
- Javadoc for all public classes and methods

## Troubleshooting Tips

1. **Always run on Windows**: This tool requires Windows for integrated authentication
2. **Check domain membership**: Verify you're logged into a domain, not a local account
3. **Review logs**: Check both console output and win-auth-tester.log file
4. **Test with IE**: Try accessing the URL in Internet Explorer to verify server configuration
5. **Check time sync**: Kerberos requires synchronized clocks (within 5 minutes)
6. **Firewall rules**: Ensure firewall allows HTTP/HTTPS traffic to the server

## License

This project is provided as-is for troubleshooting purposes.

## Support

For issues or questions:
1. Review the detailed log output
2. Check the troubleshooting recommendations
3. Consult Waffle documentation: https://github.com/Waffle/waffle
4. Review Windows authentication documentation

## Contributing

Contributions are welcome! Please ensure:
- Code follows existing style
- Tests are included
- Documentation is updated
- Logging is comprehensive

## Version History

- **1.0.0** (2026-04-28)
  - Initial release
  - Support for NTLM and Negotiate authentication
  - Comprehensive diagnostics and troubleshooting
  - Verbose logging capabilities
