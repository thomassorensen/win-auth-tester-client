package com.winauth.tester;
import static com.winauth.tester.StringUtils.repeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes failed authentication attempts and provides troubleshooting guidance.
 */
public class TroubleshootingEngine {
    private static final Logger logger = LoggerFactory.getLogger(TroubleshootingEngine.class);

    public void analyzeFailed(AuthenticationResult result) {
        logger.info(repeat("=", 80));
        logger.info("TROUBLESHOOTING ANALYSIS");
        logger.info(repeat("=", 80));

        List<String> possibleCauses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (result.getStatusCode() == 401) {
            analyzeUnauthorized(result, possibleCauses, recommendations);
        } else if (result.getException() != null) {
            analyzeException(result, possibleCauses, recommendations);
        } else {
            analyzeOtherFailure(result, possibleCauses, recommendations);
        }

        displayAnalysis(possibleCauses, recommendations);
        logger.info(repeat("=", 80));
    }

    private void analyzeUnauthorized(AuthenticationResult result, List<String> causes, List<String> recommendations) {
        logger.info("Analyzing 401 Unauthorized response...");

        if (result.getServerAuthMethods().isEmpty()) {
            causes.add("Server did not advertise any authentication methods (no WWW-Authenticate header)");
            recommendations.add("Verify that the servlet is configured with Waffle authentication filter");
            recommendations.add("Check the web.xml or servlet configuration for Waffle filter settings");
            recommendations.add("Ensure the Waffle JAR files are present in the servlet container");
            recommendations.add("Check servlet container logs for Waffle initialization errors");
        } else {
            boolean hasNtlm = false;
            boolean hasNegotiate = false;
            
            for (String method : result.getServerAuthMethods()) {
                if (method.toUpperCase().contains("NTLM")) {
                    hasNtlm = true;
                }
                if (method.toUpperCase().contains("NEGOTIATE")) {
                    hasNegotiate = true;
                }
            }

            if (!hasNtlm && !hasNegotiate) {
                causes.add("Server does not support NTLM or Negotiate authentication");
                recommendations.add("Configure Waffle to support Windows authentication protocols");
            } else {
                causes.add("Client failed to complete the authentication handshake");
                recommendations.add("Verify that you are running on a Windows machine with domain credentials");
                recommendations.add("Check if your user account has permission to access the resource");
                recommendations.add("Try running the application with explicit credentials using -U and -P options");
                recommendations.add("Verify that your user is a member of the required security groups");
            }
        }

        // Domain and network checks
        String userDomain = System.getenv("USERDOMAIN");
        if (userDomain == null || userDomain.equals(System.getenv("COMPUTERNAME"))) {
            causes.add("User may not be logged into a domain (local account detected)");
            recommendations.add("Ensure you are logged into a Windows domain account, not a local account");
            recommendations.add("Contact your system administrator to verify domain membership");
        }

        // Check for common configuration issues
        recommendations.add("Verify network connectivity to the server");
        recommendations.add("Check Windows Event Viewer for authentication errors");
        recommendations.add("Ensure system clock is synchronized with domain controller (Kerberos requirement)");
        recommendations.add("Try accessing the URL in Internet Explorer to test Windows authentication");
    }

    private void analyzeException(AuthenticationResult result, List<String> causes, List<String> recommendations) {
        Exception ex = result.getException();
        String exceptionType = ex.getClass().getSimpleName();
        String message = ex.getMessage();

        logger.info("Analyzing exception: {} - {}", exceptionType, message);

        if (exceptionType.contains("UnknownHost")) {
            causes.add("DNS resolution failed - cannot resolve hostname");
            recommendations.add("Verify the URL is correct");
            recommendations.add("Check DNS server configuration");
            recommendations.add("Try using IP address instead of hostname");
            recommendations.add("Check network connectivity and firewall rules");
        } else if (exceptionType.contains("ConnectException") || exceptionType.contains("Timeout")) {
            causes.add("Cannot establish connection to server");
            recommendations.add("Verify the server is running and accessible");
            recommendations.add("Check firewall rules on both client and server");
            recommendations.add("Verify the port number is correct");
            recommendations.add("Try increasing the timeout value with -t option");
        } else if (exceptionType.contains("SSL") || exceptionType.contains("Certificate")) {
            causes.add("SSL/TLS certificate validation failed");
            recommendations.add("Verify the server certificate is valid and trusted");
            recommendations.add("Check if certificate has expired");
            recommendations.add("Ensure certificate matches the hostname");
            recommendations.add("Import the server certificate into Java trust store if self-signed");
        } else if (message != null && message.toLowerCase().contains("credential")) {
            causes.add("Credential-related error occurred");
            recommendations.add("Verify username and password are correct");
            recommendations.add("Check if account is locked or disabled");
            recommendations.add("Ensure domain name is correct");
            recommendations.add("Try running as a different user");
        } else {
            causes.add("Unexpected error occurred: " + exceptionType);
            recommendations.add("Check application logs for detailed stack trace");
            recommendations.add("Verify all required dependencies are present");
            recommendations.add("Try running with different authentication options (-n or -g)");
        }
    }

    private void analyzeOtherFailure(AuthenticationResult result, List<String> causes, List<String> recommendations) {
        logger.info("Analyzing unexpected status code: {}", result.getStatusCode());

        if (result.getStatusCode() == 403) {
            causes.add("Access forbidden - authentication succeeded but authorization failed");
            recommendations.add("Verify your user account has permission to access the resource");
            recommendations.add("Check servlet authorization configuration");
            recommendations.add("Verify security group membership");
            recommendations.add("Contact server administrator to grant access");
        } else if (result.getStatusCode() == 404) {
            causes.add("Resource not found");
            recommendations.add("Verify the URL path is correct");
            recommendations.add("Check servlet mapping configuration");
        } else if (result.getStatusCode() == 500) {
            causes.add("Server internal error");
            recommendations.add("Check servlet container logs for errors");
            recommendations.add("Verify Waffle configuration on the server");
            recommendations.add("Contact server administrator");
        } else if (result.getStatusCode() == 0) {
            causes.add("No response received from server");
            recommendations.add("Verify server is running");
            recommendations.add("Check network connectivity");
            recommendations.add("Verify URL is accessible");
        } else {
            causes.add("Unexpected HTTP status code: " + result.getStatusCode());
            recommendations.add("Review HTTP status code meaning");
            recommendations.add("Check server logs for more information");
        }
    }

    private void displayAnalysis(List<String> causes, List<String> recommendations) {
        logger.info("");
        logger.info("POSSIBLE CAUSES:");
        logger.info(repeat("-", 80));
        for (int i = 0; i < causes.size(); i++) {
            logger.info("{}. {}", (i + 1), causes.get(i));
        }

        logger.info("");
        logger.info("RECOMMENDED ACTIONS:");
        logger.info(repeat("-", 80));
        for (int i = 0; i < recommendations.size(); i++) {
            logger.info("{}. {}", (i + 1), recommendations.get(i));
        }

        logger.info("");
        logger.info("ADDITIONAL RESOURCES:");
        logger.info(repeat("-", 80));
        logger.info("• Waffle Documentation: https://github.com/Waffle/waffle");
        logger.info("• Windows Authentication Overview: https://docs.microsoft.com/en-us/windows-server/security/");
        logger.info("• NTLM Authentication: https://docs.microsoft.com/en-us/windows/win32/secauthn/microsoft-ntlm");
        logger.info("• Kerberos Authentication: https://docs.microsoft.com/en-us/windows-server/security/kerberos/");
    }
}
