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
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");
        String registerJson = """
                {"username": "newuser", "password": "password123"}
                """;

        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.username").isEqualTo("newuser")
                .jsonPath("$.role").isEqualTo("ROLE_USER");
    }

    @Test
    void register_duplicateUsername_returnsError() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");
        String registerJson = """
                {"username": "duplicateuser", "password": "password123"}
                """;

        // First registration
        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().isOk();

        // Second registration with same username
        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().is5xxServerError();
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
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");
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
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");

        // First register a user
        String registerJson = """
                {"username": "logintest", "password": "password123"}
                """;

        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registerJson)
                .exchange()
                .expectStatus().isOk();

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
                .jsonPath("$.username").isEqualTo("logintest")
                .jsonPath("$.role").isEqualTo("ROLE_USER");
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
        String token = jwtUtil.generateToken("testuser", "ROLE_USER");

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
        String userToken = jwtUtil.generateToken("regularuser", "ROLE_USER");
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
        String adminToken = jwtUtil.generateToken("adminuser", "ROLE_ADMIN");
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
}
