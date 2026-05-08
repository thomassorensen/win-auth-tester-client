package com.winauth.tester;

import org.apache.http.Header;
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
import org.apache.http.impl.auth.win.WindowsNTLMSchemeFactory;
import org.apache.http.impl.auth.win.WindowsNegotiateSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.WinHttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HTTP client that handles Windows authentication (NTLM/Negotiate).
 */
public class WindowsAuthClient {
    private static final Logger logger = LoggerFactory.getLogger(WindowsAuthClient.class);
    
    private final String targetUrl;
    private final int timeout;

    @FunctionalInterface
    private interface HttpClientFactory {
        CloseableHttpClient create() throws Exception;
    }

    public WindowsAuthClient(String targetUrl, int timeout) {
        this.targetUrl = targetUrl;
        this.timeout = timeout;
    }

    /**
     * Tests authentication using the current Windows logged-in user.
     */
    public AuthenticationResult testAuthenticationWithCurrentUser(boolean forceNtlm, boolean forceNegotiate) {
        logger.info("Testing authentication with current Windows user...");
        
        if (forceNtlm) {
            logger.info("Forcing NTLM authentication scheme");
            return executeWithHttpsCertificateRecovery("NTLM (Current User)", new HttpClientFactory() {
                @Override
                public CloseableHttpClient create() throws Exception {
                    return createNtlmClient(null);
                }
            });
        }

        if (forceNegotiate) {
            logger.info("Forcing Negotiate authentication scheme");
            return executeWithHttpsCertificateRecovery("Negotiate (Current User)", new HttpClientFactory() {
                @Override
                public CloseableHttpClient create() throws Exception {
                    return createNegotiateClient(null);
                }
            });
        }

        logger.info("Using Windows integrated authentication (will negotiate best available)");
        return executeWithHttpsCertificateRecovery("Windows Integrated (Current User)", new HttpClientFactory() {
            @Override
            public CloseableHttpClient create() {
                return WinHttpClients.createDefault();
            }
        });
    }

    /**
     * Tests authentication using explicit credentials.
     */
    public AuthenticationResult testAuthenticationWithCredentials(String username, String password, 
                                                                   String domain, boolean forceNtlm, 
                                                                   boolean forceNegotiate) {
        logger.info("Testing authentication with explicit credentials...");

        try {
            final String workstation = InetAddress.getLocalHost().getHostName();
            final NTCredentials credentials = new NTCredentials(username, password, workstation, domain);

            logger.info("Credentials - Username: {}, Domain: {}, Workstation: {}", 
                       username, domain != null ? domain : "N/A", workstation);

            if (forceNtlm) {
                logger.info("Forcing NTLM authentication scheme");
                return executeWithHttpsCertificateRecovery("NTLM (Explicit)", new HttpClientFactory() {
                    @Override
                    public CloseableHttpClient create() throws Exception {
                        return createNtlmClient(credentials);
                    }
                });
            }

            if (forceNegotiate) {
                logger.info("Forcing Negotiate authentication scheme");
                return executeWithHttpsCertificateRecovery("Negotiate (Explicit)", new HttpClientFactory() {
                    @Override
                    public CloseableHttpClient create() throws Exception {
                        return createNegotiateClient(credentials);
                    }
                });
            }

            logger.info("Using Windows authentication with Negotiate/NTLM fallback");
            return executeWithHttpsCertificateRecovery("Explicit Credentials", new HttpClientFactory() {
                @Override
                public CloseableHttpClient create() throws Exception {
                    return createWindowsClient(credentials);
                }
            });

        } catch (Exception e) {
            AuthenticationResult result = createResult("Explicit Credentials");
            logger.error("Exception during authentication: {}", e.getMessage(), e);
            result.setSuccessful(false);
            result.setException(e);
            return result;
        }
    }

    private AuthenticationResult executeWithHttpsCertificateRecovery(String authenticationMethod,
                                                                     HttpClientFactory clientFactory) {
        AuthenticationResult result = createResult(authenticationMethod);
        boolean retriedAfterImport = false;

        while (true) {
            try (CloseableHttpClient httpClient = clientFactory.create()) {
                result = executeRequest(httpClient, result);
            } catch (Exception e) {
                logger.error("Exception during authentication: {}", e.getMessage(), e);
                result.setSuccessful(false);
                result.setException(e);
            }

            if (!retriedAfterImport && shouldOfferCertificateImport(result)) {
                CertificateImportAttempt importAttempt = tryImportServerCertificate(targetUrl);
                if (importAttempt.imported) {
                    retriedAfterImport = true;
                    logger.info("Retrying HTTPS request after importing certificate into default Java keystore...");
                    result = createResult(authenticationMethod);
                    continue;
                }

                if (importAttempt.userDeclined) {
                    logger.info("Certificate import declined by user. Continuing without modifying trust store.");
                }
            }

            return result;
        }
    }

    private AuthenticationResult createResult(String authenticationMethod) {
        AuthenticationResult result = new AuthenticationResult();
        result.setUrl(targetUrl);
        result.setAuthenticationMethod(authenticationMethod);
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

    private boolean shouldOfferCertificateImport(AuthenticationResult result) {
        if (result == null || result.getException() == null) {
            return false;
        }

        String normalizedUrl = targetUrl == null ? "" : targetUrl.toLowerCase(Locale.ROOT);
        if (!normalizedUrl.startsWith("https://")) {
            return false;
        }

        Throwable cause = result.getException();
        while (cause != null) {
            if (cause instanceof SSLHandshakeException) {
                return true;
            }

            String className = cause.getClass().getName().toLowerCase(Locale.ROOT);
            String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase(Locale.ROOT);

            if (className.contains("certificate") || className.contains("ssl")
                || message.contains("unable to find valid certification path")
                || message.contains("pkix path building failed")
                || message.contains("certificate")) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private CertificateImportAttempt tryImportServerCertificate(String urlString) {
        CertificateImportAttempt attempt = new CertificateImportAttempt();

        try {
            URL url = URI.create(urlString).toURL();
            if (!"https".equalsIgnoreCase(url.getProtocol())) {
                return attempt;
            }

            String host = url.getHost();
            int port = url.getPort() > 0 ? url.getPort() : 443;

            X509Certificate certificate = fetchLeafCertificate(host, port);
            if (certificate == null) {
                logger.warn("Could not retrieve server certificate from {}:{}", host, port);
                return attempt;
            }

            String fingerprint = sha256Fingerprint(certificate);
            logger.warn("HTTPS certificate for {}:{} is not currently trusted by Java.", host, port);
            logger.warn("Certificate details:");
            logger.warn("  Subject: {}", certificate.getSubjectX500Principal().getName());
            logger.warn("  Issuer: {}", certificate.getIssuerX500Principal().getName());
            logger.warn("  Valid From: {}", certificate.getNotBefore());
            logger.warn("  Valid To: {}", certificate.getNotAfter());
            logger.warn("  SHA-256 Fingerprint: {}", fingerprint);

            if (!promptForTrust()) {
                attempt.userDeclined = true;
                return attempt;
            }

            importCertificateIntoDefaultKeystore(host, certificate);
            attempt.imported = true;
            logger.info("Certificate imported successfully into default Java keystore.");

        } catch (Exception e) {
            logger.error("Failed to import server certificate into default Java keystore: {}", e.getMessage(), e);
        }

        return attempt;
    }

    private X509Certificate fetchLeafCertificate(String host, int port) throws Exception {
        final X509Certificate[] chainHolder = new X509Certificate[1];

        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // Not used for client auth in this flow.
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                if (chain != null && chain.length > 0) {
                    chainHolder[0] = chain[0];
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();

        try (SSLSocket socket = (SSLSocket) socketFactory.createSocket(host, port)) {
            socket.setSoTimeout(timeout);
            socket.startHandshake();
            SSLSession session = socket.getSession();
            if (chainHolder[0] == null && session != null) {
                Certificate[] peerCertificates = session.getPeerCertificates();
                if (peerCertificates != null && peerCertificates.length > 0
                    && peerCertificates[0] instanceof X509Certificate) {
                    chainHolder[0] = (X509Certificate) peerCertificates[0];
                }
            }
        } catch (SocketTimeoutException e) {
            logger.warn("Timed out while downloading certificate from {}:{}", host, port);
            throw e;
        }

        return chainHolder[0];
    }

    private void importCertificateIntoDefaultKeystore(String host, X509Certificate certificate) throws Exception {
        File keystoreFile = locateDefaultJavaKeystore();
        if (keystoreFile == null || !keystoreFile.exists()) {
            throw new IOException("Default Java keystore (cacerts) not found");
        }

        char[] password = "changeit".toCharArray();
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try (FileInputStream in = new FileInputStream(keystoreFile)) {
            keyStore.load(in, password);
        }

        if (containsCertificate(keyStore, certificate)) {
            logger.info("Certificate already exists in default Java keystore. Skipping import.");
            return;
        }

        String alias = buildCertificateAlias(host);
        keyStore.setCertificateEntry(alias, certificate);

        try (FileOutputStream out = new FileOutputStream(keystoreFile)) {
            keyStore.store(out, password);
        }
    }

    private File locateDefaultJavaKeystore() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.trim().isEmpty()) {
            return null;
        }

        File direct = new File(javaHome, "lib/security/cacerts");
        if (direct.exists()) {
            return direct;
        }

        File legacy = new File(javaHome, "jre/lib/security/cacerts");
        if (legacy.exists()) {
            return legacy;
        }

        return null;
    }

    private boolean containsCertificate(KeyStore keyStore, X509Certificate certificate) throws Exception {
        for (java.util.Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements(); ) {
            String alias = aliases.nextElement();
            Certificate existing = keyStore.getCertificate(alias);
            if (existing instanceof X509Certificate) {
                if (MessageDigest.isEqual(existing.getEncoded(), certificate.getEncoded())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String buildCertificateAlias(String host) {
        String safeHost = host == null ? "server" : host.replaceAll("[^a-zA-Z0-9._-]", "_");
        String stamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.ROOT).format(new Date());
        return "win-auth-tester-" + safeHost + "-" + stamp;
    }

    private String sha256Fingerprint(X509Certificate certificate) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02X", digest[i]));
        }
        return sb.toString();
    }

    private boolean promptForTrust() {
        String prompt = "Do you trust this certificate and want to import it into the default Java keystore? (yes/no): ";
        Console console = System.console();

        String response;
        if (console != null) {
            response = console.readLine(prompt);
        } else {
            System.out.print(prompt);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                response = reader.readLine();
            } catch (IOException e) {
                logger.warn("Could not read trust confirmation from stdin: {}", e.getMessage());
                return false;
            }
        }

        return isAffirmativeResponse(response);
    }

    static boolean isAffirmativeResponse(String value) {
        if (value == null) {
            return false;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return "y".equals(normalized) || "yes".equals(normalized);
    }

    private static class CertificateImportAttempt {
        private boolean imported;
        private boolean userDeclined;
    }

    private CloseableHttpClient createWindowsClient(Credentials credentials) throws Exception {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        
        URL url = URI.create(targetUrl).toURL();
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
        
        URL url = URI.create(targetUrl).toURL();
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
        
        URL url = URI.create(targetUrl).toURL();
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
