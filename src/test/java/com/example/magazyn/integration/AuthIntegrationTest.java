package com.example.magazyn.integration;

import com.example.magazyn.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void register_success() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");
        String registerJson = """
                {"username": "newuser", "password": "password123"}
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(registerJson, headers),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("token", "newuser", "ROLE_USER");
    }

    @Test
    void register_duplicateUsername_returnsError() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");
        String registerJson = """
                {"username": "duplicateuser", "password": "password123"}
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        HttpEntity<String> entity = new HttpEntity<>(registerJson, headers);

        // First registration
        ResponseEntity<String> first = restTemplate.postForEntity("/api/auth/register", entity, String.class);
        assertThat(first.getStatusCode().value()).isEqualTo(200);

        // Second registration with same username
        ResponseEntity<String> second = restTemplate.postForEntity("/api/auth/register", entity, String.class);
        assertThat(second.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void register_withoutAuth_returns401() {
        String registerJson = """
                {"username": "unauthuser", "password": "password123"}
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(registerJson, headers),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void register_withInvalidData_returns400() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");
        String invalidJson = """
                {"username": "ab", "password": "12"}
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(invalidJson, headers),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void login_success() {
        String adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");

        // First register a user
        String registerJson = """
                {"username": "logintest", "password": "password123"}
                """;

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.setBearerAuth(adminToken);

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "/api/auth/register",
                new HttpEntity<>(registerJson, authHeaders),
                String.class);
        assertThat(registerResponse.getStatusCode().value()).isEqualTo(200);

        // Then login
        String loginJson = """
                {"username": "logintest", "password": "password123"}
                """;

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(loginJson, loginHeaders),
                String.class);

        assertThat(loginResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(loginResponse.getBody()).contains("token", "logintest", "ROLE_USER");
    }

    @Test
    void login_invalidCredentials_returns401() {
        String loginJson = """
                {"username": "nonexistent", "password": "wrongpassword"}
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(loginJson, headers),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void accessProtectedEndpoint_withValidToken_success() {
        String token = jwtUtil.generateToken("testuser", "ROLE_USER");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void accessProtectedEndpoint_withoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/products", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void accessAdminEndpoint_withUserRole_returns403() {
        String userToken = jwtUtil.generateToken("regularuser", "ROLE_USER");
        String productJson = """
                {"name": "Test", "sku": "TST-AUTH", "unit": "szt."}
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(userToken);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/products",
                new HttpEntity<>(productJson, headers),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void accessAdminEndpoint_withAdminToken_success() {
        String adminToken = jwtUtil.generateToken("adminuser", "ROLE_ADMIN");
        String productJson = """
                {"name": "Admin Product", "sku": "ADM-001", "unit": "szt."}
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/products",
                new HttpEntity<>(productJson, headers),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void accessActuatorHealth_withoutAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
