package com.job.scheduler.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private final JwtUtil jwt = new JwtUtil();

    @Test
    void generatedTokenRoundTripsClaims() {
        String token = jwt.generateToken(42L, "owner@example.com");

        assertTrue(jwt.isTokenValid(token));
        assertEquals(42L, jwt.extractUserId(token));
        assertEquals("owner@example.com", jwt.extractEmail(token));
    }

    @Test
    void malformedOrTamperedTokenIsRejected() {
        String token = jwt.generateToken(42L, "owner@example.com");

        assertFalse(jwt.isTokenValid("not-a-jwt"));
        assertFalse(jwt.isTokenValid(token + "tampered"));
    }
}
