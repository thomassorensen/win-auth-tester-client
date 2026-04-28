# Usage Examples

This document provides detailed examples of how to use the Windows Authentication Tester Client.

## Prerequisites

- Build the project: `mvn clean package`
- Ensure you're running on Windows
- Have network access to the target servlet

## Basic Examples

### 1. Test with Current Windows User

Test authentication using your current Windows logged-in credentials:

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api/protected
```

Or using the batch script:

```batch
run.bat http://server:8080/api/protected
```

### 2. Test with Explicit Credentials

Provide username and password directly:

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api/protected -U john.doe -P mypassword -d COMPANY
```

### 3. Force NTLM Authentication

Force the use of NTLM protocol only (disable Negotiate/Kerberos):

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api/protected -n
```

### 4. Force Negotiate Authentication

Force the use of Negotiate protocol (Kerberos with NTLM fallback):

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api/protected -g
```

### 5. HTTPS Endpoint

Test against a secure HTTPS endpoint:

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u https://server.company.com/secure/api
```

### 6. Custom Timeout

Set a longer timeout for slow connections (60 seconds):

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api/protected -t 60000
```

## Advanced Examples

### 7. Test Multiple Scenarios

Test the same endpoint with different authentication methods:

```batch
REM Test with automatic authentication
run.bat http://server:8080/api/protected

REM Test forcing NTLM
run.bat http://server:8080/api/protected -n

REM Test forcing Negotiate
run.bat http://server:8080/api/protected -g

REM Test with explicit credentials
run.bat http://server:8080/api/protected -U testuser -P testpass -d TESTDOMAIN
```

### 8. Testing Different Environments

Development environment:
```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://dev-server:8080/app
```

Staging environment:
```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://staging-server:8080/app
```

Production environment:
```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u https://prod-server.company.com/app
```

### 9. Troubleshooting Connection Issues

If you're having connection problems, increase the timeout and check the logs:

```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api/protected -t 120000
```

Then review the `win-auth-tester.log` file for detailed information.

### 10. Testing from Different User Contexts

Run as a different Windows user (using runas):

```batch
runas /user:DOMAIN\username "java -jar target\win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api/protected"
```

## Understanding the Output

### Success Example

```
================================================================================
Windows Authentication Tester - Starting
================================================================================
Running pre-connection diagnostics...
--------------------------------------------------------------------------------
Operating System: Windows 10 10.0 (amd64)
Current User: john.doe
User Domain: COMPANY
Computer Name: LAPTOP-001
Target Host: server.company.com
Target Port: 8080
Target Protocol: http
✓ Host is reachable via ICMP
DNS Resolution for server.company.com:
  192.168.1.100 (Canonical: server.company.com)
--------------------------------------------------------------------------------
Testing authentication with current Windows user...
Using Windows integrated authentication (will negotiate best available)
Sending request to: http://server.company.com:8080/api/protected
Request method: GET
Response received in 245 ms
Status Code: 200
Status Reason: OK
Response Headers:
  Date: Mon, 28 Apr 2026 12:00:00 GMT
  Server: Apache-Coyote/1.1
  Content-Type: application/json
✓ Authentication SUCCESSFUL
================================================================================
AUTHENTICATION RESULTS
================================================================================
URL: http://server.company.com:8080/api/protected
Authentication Method: Windows Integrated (Current User)
Response Time: 245 ms
Status Code: 200
Status Reason: OK

✓✓✓ SUCCESS ✓✓✓
Authentication completed successfully!
================================================================================
```

### Failure Example with Troubleshooting

```
================================================================================
Windows Authentication Tester - Starting
================================================================================
Running pre-connection diagnostics...
--------------------------------------------------------------------------------
[... diagnostic output ...]
--------------------------------------------------------------------------------
Testing authentication with current Windows user...
Sending request to: http://server:8080/api/protected
Response received in 123 ms
Status Code: 401
Status Reason: Unauthorized
✗ Authentication FAILED - 401 Unauthorized
Server authentication requirements (WWW-Authenticate headers):
  Negotiate
  NTLM
================================================================================
AUTHENTICATION RESULTS
================================================================================
URL: http://server:8080/api/protected
Authentication Method: Windows Integrated (Current User)
Response Time: 123 ms
Status Code: 401
Status Reason: Unauthorized

✗✗✗ FAILURE ✗✗✗
Authentication failed!

Server Authentication Methods Advertised:
  - Negotiate
  - NTLM
================================================================================
================================================================================
TROUBLESHOOTING ANALYSIS
================================================================================
Analyzing 401 Unauthorized response...

POSSIBLE CAUSES:
--------------------------------------------------------------------------------
1. Client failed to complete the authentication handshake
2. User may not be logged into a domain (local account detected)

RECOMMENDED ACTIONS:
--------------------------------------------------------------------------------
1. Verify that you are running on a Windows machine with domain credentials
2. Check if your user account has permission to access the resource
3. Try running the application with explicit credentials using -U and -P options
4. Verify that your user is a member of the required security groups
5. Ensure you are logged into a Windows domain account, not a local account
6. Contact your system administrator to verify domain membership
7. Verify network connectivity to the server
8. Check Windows Event Viewer for authentication errors
9. Ensure system clock is synchronized with domain controller (Kerberos requirement)
10. Try accessing the URL in Internet Explorer to test Windows authentication

ADDITIONAL RESOURCES:
--------------------------------------------------------------------------------
• Waffle Documentation: https://github.com/Waffle/waffle
• Windows Authentication Overview: https://docs.microsoft.com/en-us/windows-server/security/
• NTLM Authentication: https://docs.microsoft.com/en-us/windows/win32/secauthn/microsoft-ntlm
• Kerberos Authentication: https://docs.microsoft.com/en-us/windows-server/security/kerberos/
================================================================================
```

## Interpreting Results

### Status Code 200
- ✅ Authentication successful
- You have access to the resource
- Your credentials were accepted

### Status Code 401
- ❌ Authentication failed
- Check user permissions
- Verify domain membership
- Review troubleshooting recommendations

### Status Code 403
- ❌ Authentication succeeded but authorization failed
- Your identity is verified but you don't have permission
- Contact administrator to grant access

### Status Code 404
- ⚠️ Resource not found
- Verify the URL is correct
- Check servlet mapping

### Status Code 500
- ⚠️ Server error
- Check server logs
- Contact server administrator

## Batch Testing Script Example

Create a batch file to test multiple endpoints:

```batch
@echo off
echo Testing authentication on multiple endpoints...
echo.

set JAR=target\win-auth-tester-client-1.0.0-jar-with-dependencies.jar

echo Testing Development Environment...
java -jar %JAR% -u http://dev-server:8080/api/test
echo.

echo Testing Staging Environment...
java -jar %JAR% -u http://staging-server:8080/api/test
echo.

echo Testing Production Environment...
java -jar %JAR% -u https://prod-server.company.com/api/test
echo.

echo All tests complete!
pause
```

## Tips for Effective Troubleshooting

1. **Always review the full log file** (`win-auth-tester.log`) for detailed HTTP wire logs
2. **Compare successful vs failed attempts** to identify differences
3. **Test with Internet Explorer** first to verify server configuration
4. **Check system time** - Kerberos requires clock synchronization within 5 minutes
5. **Verify domain membership** with `echo %USERDOMAIN%` command
6. **Run as different users** to isolate permission issues
7. **Test on the server itself** to rule out network issues
8. **Check Windows Event Viewer** for authentication errors

## Common Scenarios

### Scenario 1: Development Machine Not on Domain

If testing from a machine not joined to the domain:
```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api -U domain-user -P password -d DOMAIN
```

### Scenario 2: Testing Behind Corporate Firewall

Ensure the application can reach the server and increase timeout:
```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api -t 60000
```

### Scenario 3: Debugging Kerberos vs NTLM

Test with Negotiate (prefers Kerberos):
```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api -g
```

Test with NTLM only:
```bash
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u http://server:8080/api -n
```

Compare the results and log files to understand which protocol works.
