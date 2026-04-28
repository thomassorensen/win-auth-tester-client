package com.winauth.tester;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for AuthenticationResult.
 */
class AuthenticationResultTest {

    @Test
    void testAuthenticationResultCreation() {
        AuthenticationResult result = new AuthenticationResult();
        result.setUrl("http://localhost:8080/test");
        result.setStatusCode(200);
        result.setSuccessful(true);

        assertEquals("http://localhost:8080/test", result.getUrl());
        assertEquals(200, result.getStatusCode());
        assertTrue(result.isSuccessful());
    }

    @Test
    void testAuthenticationResultFailure() {
        AuthenticationResult result = new AuthenticationResult();
        result.setUrl("http://localhost:8080/test");
        result.setStatusCode(401);
        result.setSuccessful(false);

        assertEquals("http://localhost:8080/test", result.getUrl());
        assertEquals(401, result.getStatusCode());
        assertFalse(result.isSuccessful());
    }
}
