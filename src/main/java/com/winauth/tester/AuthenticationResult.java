package com.winauth.tester;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the result of an authentication attempt.
 */
public class AuthenticationResult {
    private String url;
    private String authenticationMethod;
    private boolean successful;
    private int statusCode;
    private String statusReason;
    private long responseTimeMs;
    private List<String> responseHeaders = new ArrayList<>();
    private List<String> serverAuthMethods = new ArrayList<>();
    private String responseBody;
    private Exception exception;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public List<String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(List<String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public List<String> getServerAuthMethods() {
        return serverAuthMethods;
    }

    public void setServerAuthMethods(List<String> serverAuthMethods) {
        this.serverAuthMethods = serverAuthMethods;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
