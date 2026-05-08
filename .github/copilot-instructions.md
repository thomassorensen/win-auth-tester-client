# GitHub Copilot Instructions for Windows Authentication Tester Client

## Project Overview

This is a Java diagnostic tool for troubleshooting Windows authentication (NTLM/Kerberos) with Waffle-enabled servlets. The application runs on Windows, uses Apache HttpClient for HTTP communication with Windows authentication support, and provides comprehensive logging and troubleshooting capabilities.

## Code Style and Conventions

- Use Java 11+ features where appropriate
- Follow standard Java naming conventions (camelCase for methods/variables, PascalCase for classes)
- Add comprehensive SLF4J logging at appropriate levels (DEBUG for detailed flow, INFO for key events, WARN for potential issues, ERROR for failures)
- Include Javadoc comments for all public classes and methods
- Keep methods focused and under 50 lines when possible
- Use meaningful variable names that describe the data they contain

## Key Design Principles

1. **Verbose Logging**: All authentication steps should be logged with sufficient detail for troubleshooting
2. **User-Friendly Output**: Present information in a clear, structured format with visual separators
3. **Graceful Error Handling**: Catch exceptions, log them, and provide actionable guidance
4. **Diagnostic-First**: Run diagnostics before attempting authentication
5. **Troubleshooting Focus**: When failures occur, provide specific, actionable recommendations

## Important Technical Details

### Windows Authentication
- Use Apache HttpClient with `httpclient-win` for Windows authentication
- Support both current user credentials and explicit credentials
- Handle NTLM, Negotiate (Kerberos), and SPNEGO authentication schemes
- Use `WinHttpClients.createDefault()` for automatic Windows integrated auth

### Error Handling
- Convert technical errors into user-friendly messages in the TroubleshootingEngine
- Include failure context (for example: URL, status code, auth scheme, and exception message) so recommendations are specific

### Logging Configuration
- Use Logback for logging implementation
- Configure both console and file appenders
- Enable wire logging for HTTP traffic (`org.apache.http.wire`)
- Use DEBUG level for development, INFO for production

### Testing
- Use JUnit 5 for unit tests
- Mock external dependencies where appropriate
- Test both success and failure scenarios

## Common Patterns

### HTTP Request Pattern
```java
CloseableHttpClient client = createClient();
HttpGet request = new HttpGet(url);
HttpResponse response = client.execute(request, context);
// Process response
client.close();
```

### Result Handling Pattern
```java
AuthenticationResult result = new AuthenticationResult();
result.setUrl(url);
// ... populate result
if (result.isSuccessful()) {
    logger.info("✓ Success");
} else {
    logger.error("✗ Failure");
    troubleshooter.analyzeFailed(result);
}
```

### Diagnostic Check Pattern
```java
private void checkSomething() {
    logger.info("Checking something...");
    try {
        // Perform check
        logger.info("✓ Check passed: details");
    } catch (Exception e) {
        logger.error("✗ Check failed: {}", e.getMessage());
    }
}
```

## Dependencies to Use

- **Apache HttpClient**: For HTTP communication
- **JNA**: For Windows native API access (user info, security)
- **SLF4J + Logback**: For logging
- **Apache Commons CLI**: For command-line parsing
- **JUnit 5**: For testing

## What NOT to Do

- Don't add dependencies for functionality already provided by existing libraries
- Don't suppress exceptions without logging them
- Don't use System.out.println(); always use SLF4J logger
- Don't create overly complex class hierarchies
- Don't hardcode values that should be configurable
- Don't commit credentials or sensitive information

## Module-Specific Guidance

### WindowsAuthClient
- Focus on HTTP communication and authentication
- Log all request/response details
- Handle different authentication schemes
- Measure and report response times

### DiagnosticRunner
- Run checks that don't require external connections first
- Each check should be independent
- Log results clearly with ✓ or ✗ symbols
- Don't fail fast - run all diagnostics

### TroubleshootingEngine
- Provide multiple possible causes
- Give specific, actionable recommendations
- Include links to relevant documentation
- Tailor advice to the specific failure mode

### ResultPresenter
- Use visual separators (=, -)
- Highlight success/failure clearly
- Show all relevant information concisely
- Format output for easy reading

## Adding New Features

When adding features:
1. Consider adding a command-line option if user-facing
2. Update the README with usage examples
3. Add unit tests for the new functionality
4. Update troubleshooting recommendations if relevant
5. Consider adding or refining diagnostic checks when relevant

## Debugging Tips

- Enable wire logging to see raw HTTP traffic
- Check win-auth-tester.log for full details
- Run diagnostics first to identify environment issues
- Test with Internet Explorer to isolate server vs. client issues
- Verify Windows domain membership and permissions
- Check system time synchronization for Kerberos

## Security Considerations

- Never log passwords or sensitive credentials
- Use secure credential storage mechanisms
- Warn users about passing credentials on command line
- Clear sensitive data from memory when possible
- Follow principle of least privilege
