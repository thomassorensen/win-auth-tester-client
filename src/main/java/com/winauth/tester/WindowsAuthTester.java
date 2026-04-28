package com.winauth.tester;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application for testing Windows authentication against Waffle-enabled servlets.
 * Provides verbose logging and diagnostic capabilities.
 */
public class WindowsAuthTester {
    private static final Logger logger = LoggerFactory.getLogger(WindowsAuthTester.class);

    public static void main(String[] args) {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                formatter.printHelp("win-auth-tester", options);
                return;
            }

            String url = cmd.getOptionValue("url");
            if (url == null) {
                logger.error("URL is required. Use -u or --url option.");
                formatter.printHelp("win-auth-tester", options);
                System.exit(1);
            }

            boolean useNtlm = cmd.hasOption("ntlm");
            boolean useNegotiate = cmd.hasOption("negotiate");
            String username = cmd.getOptionValue("username");
            String password = cmd.getOptionValue("password");
            String domain = cmd.getOptionValue("domain");
            int timeout = Integer.parseInt(cmd.getOptionValue("timeout", "30000"));

            logger.info("=".repeat(80));
            logger.info("Windows Authentication Tester - Starting");
            logger.info("=".repeat(80));

            WindowsAuthClient client = new WindowsAuthClient(url, timeout);
            
            // Run diagnostics
            DiagnosticRunner diagnostics = new DiagnosticRunner();
            diagnostics.runPreConnectionDiagnostics(url);

            // Attempt authentication
            AuthenticationResult result;
            if (username != null && password != null) {
                logger.info("Using explicit credentials: {}\\{}", domain != null ? domain : "WORKSTATION", username);
                result = client.testAuthenticationWithCredentials(username, password, domain, useNtlm, useNegotiate);
            } else {
                logger.info("Using current Windows logged-in user credentials");
                result = client.testAuthenticationWithCurrentUser(useNtlm, useNegotiate);
            }

            // Display results
            ResultPresenter presenter = new ResultPresenter();
            presenter.displayResults(result);

            // Run troubleshooting if authentication failed
            if (!result.isSuccessful()) {
                TroubleshootingEngine troubleshooter = new TroubleshootingEngine();
                troubleshooter.analyzeFailed(result);
            }

            logger.info("=".repeat(80));
            logger.info("Windows Authentication Tester - Complete");
            logger.info("=".repeat(80));

            System.exit(result.isSuccessful() ? 0 : 1);

        } catch (ParseException e) {
            logger.error("Failed to parse command line arguments: {}", e.getMessage());
            formatter.printHelp("win-auth-tester", options);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            System.exit(1);
        }
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption(Option.builder("u")
                .longOpt("url")
                .hasArg()
                .desc("Target servlet URL (required)")
                .required(false)
                .build());

        options.addOption(Option.builder("n")
                .longOpt("ntlm")
                .hasArg(false)
                .desc("Force NTLM authentication only")
                .build());

        options.addOption(Option.builder("g")
                .longOpt("negotiate")
                .hasArg(false)
                .desc("Force Negotiate (Kerberos/NTLM) authentication only")
                .build());

        options.addOption(Option.builder("U")
                .longOpt("username")
                .hasArg()
                .desc("Username for authentication (optional)")
                .build());

        options.addOption(Option.builder("P")
                .longOpt("password")
                .hasArg()
                .desc("Password for authentication (optional)")
                .build());

        options.addOption(Option.builder("d")
                .longOpt("domain")
                .hasArg()
                .desc("Domain for authentication (optional)")
                .build());

        options.addOption(Option.builder("t")
                .longOpt("timeout")
                .hasArg()
                .desc("Connection timeout in milliseconds (default: 30000)")
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .hasArg(false)
                .desc("Display this help message")
                .build());

        return options;
    }
}
