package com.winauth.tester;
import static com.winauth.tester.StringUtils.repeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Runs pre-connection diagnostics to identify potential issues.
 */
public class DiagnosticRunner {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticRunner.class);

    public void runPreConnectionDiagnostics(String targetUrl) {
        logger.info("Running pre-connection diagnostics...");
        logger.info(repeat("-", 80));

        checkOperatingSystem();
        checkCurrentUser();
        checkNetworkConnectivity(targetUrl);
        checkDnsResolution(targetUrl);
        checkWindowsSecuritySettings();

        logger.info(repeat("-", 80));
    }

    private void checkOperatingSystem() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");

        logger.info("Operating System: {} {} ({})", osName, osVersion, osArch);

        if (!osName.toLowerCase().contains("windows")) {
            logger.warn("⚠ WARNING: This tool is designed for Windows. Current OS is: {}", osName);
            logger.warn("  Windows authentication may not work properly on non-Windows platforms.");
        }
    }

    private void checkCurrentUser() {
        try {
            String username = System.getProperty("user.name");
            String userDomain = System.getenv("USERDOMAIN");
            String computerName = System.getenv("COMPUTERNAME");
            
            logger.info("Current User: {}", username);
            logger.info("User Domain: {}", userDomain != null ? userDomain : "N/A");
            logger.info("Computer Name: {}", computerName != null ? computerName : "N/A");

            // Try to get more detailed Windows user information
            try {
                String userPrincipalName = Advapi32Util.getUserName();
                logger.info("User Principal: {}", userPrincipalName);
            } catch (Win32Exception | UnsatisfiedLinkError e) {
                logger.debug("Could not retrieve extended user information: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.warn("Could not retrieve current user information: {}", e.getMessage());
        }
    }

    private void checkNetworkConnectivity(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            String host = url.getHost();
            int port = url.getPort() != -1 ? url.getPort() : (url.getProtocol().equals("https") ? 443 : 80);

            logger.info("Target Host: {}", host);
            logger.info("Target Port: {}", port);
            logger.info("Target Protocol: {}", url.getProtocol());

            // Basic reachability check
            InetAddress address = InetAddress.getByName(host);
            logger.info("Target IP: {}", address.getHostAddress());
            
            boolean reachable = address.isReachable(5000);
            if (reachable) {
                logger.info("✓ Host is reachable via ICMP");
            } else {
                logger.warn("⚠ Host did not respond to ICMP ping (may be blocked by firewall)");
            }

        } catch (Exception e) {
            logger.error("✗ Network connectivity check failed: {}", e.getMessage());
        }
    }

    private void checkDnsResolution(String targetUrl) {
        try {
            URL url = new URL(targetUrl);
            String host = url.getHost();

            InetAddress[] addresses = InetAddress.getAllByName(host);
            logger.info("DNS Resolution for {}:", host);
            for (InetAddress addr : addresses) {
                logger.info("  {} (Canonical: {})", addr.getHostAddress(), addr.getCanonicalHostName());
            }

        } catch (UnknownHostException e) {
            logger.error("✗ DNS resolution failed for host: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("✗ DNS check failed: {}", e.getMessage());
        }
    }

    private void checkWindowsSecuritySettings() {
        try {
            // Check Java security properties
            String authSchemes = System.getProperty("http.auth.preference");
            if (authSchemes != null) {
                logger.info("Java Auth Schemes Preference: {}", authSchemes);
            }

            // Check if running as administrator
            String isAdmin = System.getenv("SESSIONNAME");
            logger.debug("Session Name: {}", isAdmin);

            // Check relevant environment variables
            String[] relevantVars = {"USERDNSDOMAIN", "LOGONSERVER", "PROCESSOR_ARCHITECTURE"};
            for (String var : relevantVars) {
                String value = System.getenv(var);
                if (value != null) {
                    logger.info("{}: {}", var, value);
                }
            }

        } catch (Exception e) {
            logger.warn("Could not check all Windows security settings: {}", e.getMessage());
        }
    }
}
