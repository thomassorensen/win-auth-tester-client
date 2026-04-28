package com.winauth.tester;
import static com.winauth.tester.StringUtils.repeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Presents authentication results in a user-friendly format.
 */
public class ResultPresenter {
    private static final Logger logger = LoggerFactory.getLogger(ResultPresenter.class);

    public void displayResults(AuthenticationResult result) {
        logger.info(repeat("=", 80));
        logger.info("AUTHENTICATION RESULTS");
        logger.info(repeat("=", 80));

        logger.info("URL: {}", result.getUrl());
        logger.info("Authentication Method: {}", result.getAuthenticationMethod());
        logger.info("Response Time: {} ms", result.getResponseTimeMs());
        logger.info("Status Code: {}", result.getStatusCode());
        logger.info("Status Reason: {}", result.getStatusReason());

        if (result.isSuccessful()) {
            logger.info("");
            logger.info("✓✓✓ SUCCESS ✓✓✓");
            logger.info("Authentication completed successfully!");
        } else {
            logger.info("");
            logger.error("✗✗✗ FAILURE ✗✗✗");
            logger.error("Authentication failed!");
            
            if (result.getException() != null) {
                logger.error("Exception: {} - {}", 
                           result.getException().getClass().getSimpleName(),
                           result.getException().getMessage());
            }
        }

        if (!result.getServerAuthMethods().isEmpty()) {
            logger.info("");
            logger.info("Server Authentication Methods Advertised:");
            for (String method : result.getServerAuthMethods()) {
                logger.info("  - {}", method);
            }
        }

        logger.info(repeat("=", 80));
    }
}
