# Windows Authentication Tester - Quick Reference

## Installation
```bash
mvn clean package
```

## Basic Usage
```bash
# Test with current Windows user
java -jar target/win-auth-tester-client-1.0.0-jar-with-dependencies.jar -u <URL>

# Or use the batch script
run.bat <URL>
```

## Command-Line Options

| Option | Description | Example |
|--------|-------------|---------|
| `-u, --url` | Target servlet URL (required) | `-u http://server:8080/api` |
| `-n, --ntlm` | Force NTLM authentication | `-n` |
| `-g, --negotiate` | Force Negotiate (Kerberos) | `-g` |
| `-U, --username` | Username for authentication | `-U john.doe` |
| `-P, --password` | Password for authentication | `-P mypassword` |
| `-d, --domain` | Domain name | `-d COMPANY` |
| `-t, --timeout` | Timeout in milliseconds | `-t 60000` |
| `-h, --help` | Show help message | `-h` |

## Common Commands

### Test with current user
```bash
run.bat http://server:8080/api/protected
```

### Test with credentials
```bash
run.bat http://server:8080/api/protected -U username -P password -d DOMAIN
```

### Force NTLM
```bash
run.bat http://server:8080/api/protected -n
```

### Force Negotiate
```bash
run.bat http://server:8080/api/protected -g
```

### HTTPS endpoint
```bash
run.bat https://server.company.com/api/protected
```

### Increase timeout
```bash
run.bat http://server:8080/api/protected -t 120000
```

## Output Files

- **Console**: Real-time output with status and results
- **win-auth-tester.log**: Detailed logs including HTTP wire traces

## Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Authentication worked! |
| 401 | Unauthorized | Check credentials/permissions |
| 403 | Forbidden | Check authorization/group membership |
| 404 | Not Found | Verify URL is correct |
| 500 | Server Error | Check server logs |

## Troubleshooting Quick Tips

1. ✓ **Check domain membership**: `echo %USERDOMAIN%`
2. ✓ **Verify time sync**: Kerberos requires ≤5 min difference
3. ✓ **Test in IE**: Internet Explorer uses Windows auth natively
4. ✓ **Review logs**: Check `win-auth-tester.log` for details
5. ✓ **Check server**: Verify Waffle filter is configured
6. ✓ **Try explicit credentials**: Use `-U`, `-P`, `-d` options
7. ✓ **Force protocol**: Use `-n` for NTLM or `-g` for Negotiate
8. ✓ **Check network**: Verify firewall allows HTTP/HTTPS

## Common Issues

### "No WWW-Authenticate header"
→ Server not configured for authentication  
→ Check Waffle filter in web.xml

### "401 with WWW-Authenticate"
→ Credentials not accepted  
→ Check domain membership and permissions

### "Connection timeout"
→ Cannot reach server  
→ Check firewall and network connectivity

### "DNS resolution failed"
→ Hostname not found  
→ Verify URL or use IP address

## Documentation

- **README.md**: Complete documentation
- **EXAMPLES.md**: Detailed usage examples
- **.github-copilot-instructions.md**: Development guidelines
- **.github/copilot-agent.md**: Expert agent for assistance

## Support Resources

- Waffle: https://github.com/Waffle/waffle
- Windows Auth: https://docs.microsoft.com/windows-server/security/
- NTLM: https://docs.microsoft.com/windows/win32/secauthn/microsoft-ntlm
- Kerberos: https://docs.microsoft.com/windows-server/security/kerberos/

## Development

```bash
# Build
mvn clean package

# Run tests
mvn test

# Generate docs
mvn javadoc:javadoc

# Clean
mvn clean
```

## Version

**Current Version**: 1.0.0  
**Java Version**: 11+  
**Platform**: Windows only (for integrated authentication)
