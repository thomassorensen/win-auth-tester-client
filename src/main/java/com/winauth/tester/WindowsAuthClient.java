package com.winauth.tester;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.auth.win.WindowsNTLMSchemeFactory;
import org.apache.http.impl.auth.win.WindowsNegotiateSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.WinHttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client that handles Windows authentication (NTLM/Negotiate).
 */
public class WindowsAuthClient {
    private static final Logger logger = LoggerFactory.getLogger(WindowsAuthClient.class);
    
    private final String targetUrl;
    private final int timeout;

    public WindowsAuthClient(String targetUrl, int timeout) {
        this.targetUrl = targetUrl;
        this.timeout = timeout;
    }

    /**
     * Tests authentication using the current Windows logged-in user.
     */
    public AuthenticationResult testAuthenticationWithCurrentUser(boolean forceNtlm, boolean forceNegotiate) {
        logger.info("Testing authentication with current Windows user...");
        
        AuthenticationResult result = new AuthenticationResult();
        result.setUrl(targetUrl);
        result.setAuthenticationMethod("Windows Integrated (Current User)");

        try {
            // Use WinHttpClients for automatic Windows authentication
            CloseableHttpClient httpClient;
            
            if (forceNtlm) {
                logger.info("Forcing NTLM authentication scheme");
                httpClient = createNtlmClient(null);
                result.setAuthenticationMethod("NTLM (Current User)");
            } else if (forceNegotiate) {
                logger.info("Forcing Negotiate authentication scheme");
                httpClient = createNegotiateClient(null);
                result.setAuthenticationMethod("Negotiate (Current User)");
            } else {
                logger.info("Using Windows integrated authentication (will negotiate best available)");
                httpClient = WinHttpClients.createDefault();
            }

            result = executeRequest(httpClient, result);
            httpClient.close();

        } catch (Exception e) {
            logger.error("Exception during authentication: {}", e.getMessage(), e);
            result.setSuccessful(false);
            result.setException(e);
        }

        return result;
    }

    /**
     * Tests authentication using explicit credentials.
     */
    public AuthenticationResult testAuthenticationWithCredentials(String username, String password, 
                                                                   String domain, boolean forceNtlm, 
                                                                   boolean forceNegotiate) {
        logger.info("Testing authentication with explicit credentials...");
        
        AuthenticationResult result = new AuthenticationResult();
        result.setUrl(targetUrl);
        result.setAuthenticationMethod("Explicit Credentials");

        try {
            String workstation = InetAddress.getLocalHost().getHostName();
            NTCredentials credentials = new NTCredentials(username, password, workstation, domain);
            
            logger.info("Credentials - Username: {}, Domain: {}, Workstation: {}", 
                       username, domain != null ? domain : "N/A", workstation);

            CloseableHttpClient httpClient;
            
            if (forceNtlm) {
                logger.info("Forcing NTLM authentication scheme");
                httpClient = createNtlmClient(credentials);
                result.setAuthenticationMethod("NTLM (Explicit)");
            } else if (forceNegotiate) {
                logger.info("Forcing Negotiate authentication scheme");
                httpClient = createNegotiateClient(credentials);
                result.setAuthenticationMethod("Negotiate (Explicit)");
            } else {
                logger.info("Using Windows authentication with Negotiate/NTLM fallback");
                httpClient = createWindowsClient(credentials);
            }

            result = executeRequest(httpClient, result);
            httpClient.close();

        } catch (Exception e) {
            logger.error("Exception during authentication: {}", e.getMessage(), e);
            result.setSuccessful(false);
            result.setException(e);
        }

        return result;
    }

    private AuthenticationResult executeRequest(CloseableHttpClient httpClient, AuthenticationResult result) {
        HttpClientContext context = HttpClientContext.create();
        HttpGet httpGet = new HttpGet(targetUrl);
        
        logger.info("Sending request to: {}", targetUrl);
        logger.info("Request method: GET");

        try {
            long startTime = System.currentTimeMillis();
            HttpResponse response = httpClient.execute(httpGet, context);
            long endTime = System.currentTimeMillis();
            
            result.setResponseTimeMs(endTime - startTime);
            result.setStatusCode(response.getStatusLine().getStatusCode());
            result.setStatusReason(response.getStatusLine().getReasonPhrase());

            logger.info("Response received in {} ms", result.getResponseTimeMs());
            logger.info("Status Code: {}", result.getStatusCode());
            logger.info("Status Reason: {}", result.getStatusReason());

            // Log all response headers
            logger.info("Response Headers:");
            List<String> responseHeaders = new ArrayList<>();
            for (Header header : response.getAllHeaders()) {
                logger.info("  {}: {}", header.getName(), header.getValue());
                responseHeaders.add(header.getName() + ": " + header.getValue());
            }
            result.setResponseHeaders(responseHeaders);

            // Log authentication scheme used
            if (context.getAuthCache() != null) {
                logger.info("Authentication cache populated: {}", context.getAuthCache());
            }

            // Read response body
            if (response.getEntity() != null) {
                String responseBody = EntityUtils.toString(response.getEntity());
                result.setResponseBody(responseBody);
                logger.debug("Response body length: {} characters", responseBody.length());
            }

            // Check if authentication was successful
            if (result.getStatusCode() == 200) {
                result.setSuccessful(true);
                logger.info("✓ Authentication SUCCESSFUL");
            } else if (result.getStatusCode() == 401) {
                result.setSuccessful(false);
                logger.error("✗ Authentication FAILED - 401 Unauthorized");
                
                // Log WWW-Authenticate headers for troubleshooting
                Header[] authHeaders = response.getHeaders("WWW-Authenticate");
                if (authHeaders.length > 0) {
                    logger.info("Server authentication requirements (WWW-Authenticate headers):");
                    List<String> authMethods = new ArrayList<>();
                    for (Header header : authHeaders) {
                        logger.info("  {}", header.getValue());
                        authMethods.add(header.getValue());
                    }
                    result.setServerAuthMethods(authMethods);
                } else {
                    logger.warn("No WWW-Authenticate header in 401 response - server may not be configured for authentication");
                }
            } else {
                result.setSuccessful(false);
                logger.warn("Unexpected status code: {}", result.getStatusCode());
            }

        } catch (IOException e) {
            logger.error("I/O error during request execution: {}", e.getMessage(), e);
            result.setSuccessful(false);
            result.setException(e);
        }

        return result;
    }

    private CloseableHttpClient createWindowsClient(Credentials credentials) throws Exception {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        
        URL url = new URL(targetUrl);
        String host = url.getHost();
        int port = url.getPort() != -1 ? url.getPort() : (url.getProtocol().equals("https") ? 443 : 80);
        
        credentialsProvider.setCredentials(
            new AuthScope(host, port),
            credentials
        );

        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
            .register(AuthSchemes.NTLM, new WindowsNTLMSchemeFactory(null))
            .register(AuthSchemes.SPNEGO, new WindowsNegotiateSchemeFactory(null))
            .register(AuthSchemes.BASIC, new BasicSchemeFactory())
            .register(AuthSchemes.DIGEST, new DigestSchemeFactory())
            .build();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .build();

        return HttpClientBuilder.create()
            .setDefaultCredentialsProvider(credentialsProvider)
            .setDefaultAuthSchemeRegistry(authSchemeRegistry)
            .setDefaultRequestConfig(requestConfig)
            .build();
    }

    private CloseableHttpClient createNtlmClient(Credentials credentials) throws Exception {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        
        URL url = new URL(targetUrl);
        String host = url.getHost();
        int port = url.getPort() != -1 ? url.getPort() : (url.getProtocol().equals("https") ? 443 : 80);
        
        if (credentials != null) {
            credentialsProvider.setCredentials(
                new AuthScope(host, port),
                credentials
            );
        }

        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
            .register(AuthSchemes.NTLM, new WindowsNTLMSchemeFactory(null))
            .build();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .build();

        HttpClientBuilder builder = HttpClientBuilder.create()
            .setDefaultAuthSchemeRegistry(authSchemeRegistry)
            .setDefaultRequestConfig(requestConfig);

        if (credentials != null) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        return builder.build();
    }

    private CloseableHttpClient createNegotiateClient(Credentials credentials) throws Exception {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        
        URL url = new URL(targetUrl);
        String host = url.getHost();
        int port = url.getPort() != -1 ? url.getPort() : (url.getProtocol().equals("https") ? 443 : 80);
        
        if (credentials != null) {
            credentialsProvider.setCredentials(
                new AuthScope(host, port),
                credentials
            );
        }

        Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
            .register(AuthSchemes.SPNEGO, new WindowsNegotiateSchemeFactory(null))
            .register(AuthSchemes.KERBEROS, new KerberosSchemeFactory())
            .build();

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setSocketTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .build();

        HttpClientBuilder builder = HttpClientBuilder.create()
            .setDefaultAuthSchemeRegistry(authSchemeRegistry)
            .setDefaultRequestConfig(requestConfig);

        if (credentials != null) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        return builder.build();
    }
}
