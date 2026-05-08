package com.winauth.tester;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowsAuthClientTest {

    @Test
    void isAffirmativeResponseAcceptsYesValues() {
        assertTrue(WindowsAuthClient.isAffirmativeResponse("yes"));
        assertTrue(WindowsAuthClient.isAffirmativeResponse("YES"));
        assertTrue(WindowsAuthClient.isAffirmativeResponse("y"));
        assertTrue(WindowsAuthClient.isAffirmativeResponse("  yEs  "));
    }

    @Test
    void isAffirmativeResponseRejectsNonYesValues() {
        assertFalse(WindowsAuthClient.isAffirmativeResponse(null));
        assertFalse(WindowsAuthClient.isAffirmativeResponse(""));
        assertFalse(WindowsAuthClient.isAffirmativeResponse("no"));
        assertFalse(WindowsAuthClient.isAffirmativeResponse("n"));
        assertFalse(WindowsAuthClient.isAffirmativeResponse("true"));
    }
}
