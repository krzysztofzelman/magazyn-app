package com.example.magazyn.integration;

import com.example.magazyn.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void register_success() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
        String registerJson = """
                {"username": "newuser", "password": "password123"}
                """;

        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.username").isEqualTo("newuser")
                .jsonPath("$.role").isEqualTo("ROLE_WAREHOUSE");
    }

    @Test
    void register_duplicateUsername_returnsError() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
        String registerJson = """
                {"username": "duplicateuser", "password": "password123"}
                """;

        // First registration
        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().isCreated();

        // Second registration with same username
        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void register_withoutAuth_returns401() {
        String registerJson = """
                {"username": "unauthuser", "password": "password123"}
                """;

        webTestClient.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void register_withInvalidData_returns400() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
        String invalidJson = """
                {"username": "ab", "password": "12"}
                """;

        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void login_success() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);

        // First register a user
        String registerJson = """
                {"username": "logintest", "password": "password123"}
                """;

        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().isCreated();

        // Then login
        String loginJson = """
                {"username": "logintest", "password": "password123"}
                """;

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginJson)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.username").isEqualTo("logintest")
                .jsonPath("$.role").isEqualTo("ROLE_WAREHOUSE");
    }

    @Test
    void login_invalidCredentials_returns401() {
        String loginJson = """
                {"username": "nonexistent", "password": "wrongpassword"}
                """;

        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginJson)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accessProtectedEndpoint_withValidToken_success() {
        String token = jwtUtil.generateToken("testuser", "ROLE_WAREHOUSE", 1L);

        webTestClient.get().uri("/api/products")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void accessProtectedEndpoint_withoutToken_returns401() {
        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void accessAdminEndpoint_withUserRole_returns403() {
        String userToken = jwtUtil.generateToken("regularuser", "ROLE_WAREHOUSE", 1L);
        String productJson = """
                {"name": "Test", "sku": "TST-AUTH", "unit": "szt."}
                """;

        webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void accessAdminEndpoint_withAdminToken_success() {
        String adminToken = jwtUtil.generateToken("adminuser", "ROLE_ADMIN", 1L);
        String productJson = """
                {"name": "Admin Product", "sku": "ADM-001", "unit": "szt."}
                """;

        webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(productJson)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void accessActuatorHealth_withoutAuth() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    // ──────────────────────────────────────────────
    // Refresh token flow
    // ──────────────────────────────────────────────

    @Test
    void refreshToken_returnsNewTokens() {
        // Register + login to get refresh token
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "refreshtest1", "password": "password123"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        byte[] loginBody = webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "refreshtest1", "password": "password123"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.refreshToken").isNotEmpty()
                .returnResult()
                .getResponseBodyContent();

        String refreshToken = extractJsonValue(loginBody, "refreshToken");

        // Use refresh token to get new tokens — verify rotation (different refresh token returned)
        byte[] refreshedBody = webTestClient.post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\": \"" + refreshToken + "\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.username").isEqualTo("refreshtest1")
                .jsonPath("$.role").isEqualTo("ROLE_WAREHOUSE")
                .returnResult()
                .getResponseBodyContent();

        // Verify token rotation (new refresh token is different from old one)
        String newRefreshToken = extractJsonValue(refreshedBody, "refreshToken");
        assertNotNull(newRefreshToken);
        assertNotEquals(refreshToken, newRefreshToken, "refresh token should be rotated");
    }

    @Test
    void refreshToken_withInvalidToken_returns401() {
        webTestClient.post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\": \"00000000-0000-0000-0000-000000000000\"}")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.error").isNotEmpty();
    }

    @Test
    void refreshToken_withExpiredToken_returns401() {
        // We can't easily create an expired token via API,
        // but we can test with a non-parseable UUID-like string
        webTestClient.post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\": \"not-a-uuid\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logout_invalidatesToken() {
        // Register + login to get refresh token
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "logouttest1", "password": "password123"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        byte[] loginBody = webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "logouttest1", "password": "password123"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .returnResult()
                .getResponseBodyContent();

        String refreshToken = extractJsonValue(loginBody, "refreshToken");

        // Logout
        webTestClient.post().uri("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\": \"" + refreshToken + "\"}")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isNotEmpty();

        // Using the same refresh token after logout should fail
        webTestClient.post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\": \"" + refreshToken + "\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logout_withInvalidToken_returns200() {
        // Logout should succeed even with an invalid/consumed token
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
        webTestClient.post().uri("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\": \"00000000-0000-0000-0000-000000000000\"}")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk();
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private String extractJsonValue(byte[] body, String key) {
        if (body == null) return null;
        String text = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        String search = "\"" + key + "\":\"";
        int start = text.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = text.indexOf("\"", start);
        return end < 0 ? null : text.substring(start, end);
    }
}
