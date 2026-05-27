package com.example.magazyn.service;

import com.example.magazyn.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "testSecretKeyForJWTThatIsAtLeast32CharactersLong";
    private static final long EXPIRATION = 3600000L; // 1 hour

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    void generateToken_returnsNonEmptyToken() {
        String token = jwtUtil.generateToken("testuser", "ROLE_USER");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token = jwtUtil.generateToken("john.doe", "ROLE_ADMIN");

        String username = jwtUtil.extractUsername(token);

        assertEquals("john.doe", username);
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken("admin", "ROLE_ADMIN");

        String role = jwtUtil.extractRole(token);

        assertEquals("ROLE_ADMIN", role);
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        String token = jwtUtil.generateToken("testuser", "ROLE_USER");

        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void isTokenValid_withInvalidToken_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("invalid.token.here"));
    }

    @Test
    void isTokenValid_withMalformedToken_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("not-a-jwt-token"));
    }

    @Test
    void isTokenValid_withEmptyToken_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid(""));
    }

    @Test
    void generateToken_differentUsers_differentTokens() {
        String token1 = jwtUtil.generateToken("user1", "ROLE_USER");
        String token2 = jwtUtil.generateToken("user2", "ROLE_USER");

        assertNotEquals(token1, token2);
    }

    @Test
    void generateToken_differentRoles_differentTokens() {
        String token1 = jwtUtil.generateToken("user", "ROLE_USER");
        String token2 = jwtUtil.generateToken("user", "ROLE_ADMIN");

        assertNotEquals(token1, token2);
    }

    @Test
    void extractUsername_withDifferentUsernames() {
        String token1 = jwtUtil.generateToken("alice", "ROLE_USER");
        String token2 = jwtUtil.generateToken("bob", "ROLE_ADMIN");

        assertEquals("alice", jwtUtil.extractUsername(token1));
        assertEquals("bob", jwtUtil.extractUsername(token2));
    }

    @Test
    void expiredToken_returnsInvalid() throws InterruptedException {
        // Create a JwtUtil with very short expiration
        JwtUtil shortLived = new JwtUtil(SECRET, 1L); // 1 ms
        String token = shortLived.generateToken("testuser", "ROLE_USER");

        // Wait for expiration
        Thread.sleep(10);

        assertFalse(shortLived.isTokenValid(token));
    }
}
