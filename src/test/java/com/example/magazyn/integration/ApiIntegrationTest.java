package com.example.magazyn.integration;

import com.example.magazyn.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private static Long createdProductId;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
    }

    // ──────────────────────────────────────────────
    // 1. Login & Logout flow
    // ──────────────────────────────────────────────

    @Test
    @Order(1)
    void login_success() {
        // Pre-register a user via admin
        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "apitest_user", "password": "password123"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        // Login
        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "apitest_user", "password": "password123"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.username").isEqualTo("apitest_user")
                .jsonPath("$.role").isEqualTo("ROLE_WAREHOUSE");
    }

    @Test
    @Order(2)
    void login_invalidCredentials_returns401() {
        webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "nonexistent", "password": "wrong"}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(3)
    void logout_success() {
        // First register + login
        webTestClient.post().uri("/api/auth/register")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "logout_user", "password": "password123"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        byte[] loginBody = webTestClient.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"username": "logout_user", "password": "password123"}
                        """)
                .exchange()
                .expectStatus().isOk()
                .returnResult()
                .getResponseBodyContent();

        String refreshToken = extractJsonValue(loginBody, "refreshToken");
        assertNotNull(refreshToken);

        // Logout
        webTestClient.post().uri("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\": \"" + refreshToken + "\"}")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.message").isNotEmpty();

        // Refresh with consumed token should fail
        webTestClient.post().uri("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"refreshToken\": \"" + refreshToken + "\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ──────────────────────────────────────────────
    // 2. Product CRUD
    // ──────────────────────────────────────────────

    @Test
    @Order(10)
    void createProduct_success() {
        String body = """
                {"name": "Test Product", "sku": "API-TST-001", "unit": "szt.", "quantity": 10, "price": 99.99}
                """;

        byte[] response = webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Test Product")
                .jsonPath("$.sku").isEqualTo("API-TST-001")
                .returnResult()
                .getResponseBodyContent();

        createdProductId = extractJsonLong(response, "id");
        assertNotNull(createdProductId);
    }

    @Test
    @Order(11)
    void createProduct_withoutAuth_returns401() {
        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "No Auth", "sku": "NO-AUTH", "unit": "szt."}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(12)
    void getProducts_returnsList() {
        webTestClient.get().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();
    }

    @Test
    @Order(13)
    void getProductById_success() {
        assertNotNull(createdProductId);

        webTestClient.get().uri("/api/products/" + createdProductId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(createdProductId)
                .jsonPath("$.name").isEqualTo("Test Product");
    }

    @Test
    @Order(14)
    void updateProduct_success() {
        assertNotNull(createdProductId);

        webTestClient.put().uri("/api/products/" + createdProductId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"name": "Updated Product", "sku": "API-TST-001", "unit": "szt.", "quantity": 20, "price": 49.99}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Updated Product")
                .jsonPath("$.quantity").isEqualTo(0); // quantity managed via warehouse documents, not update endpoint
    }

    @Test
    @Order(15)
    void deleteProduct_success() {
        assertNotNull(createdProductId);

        webTestClient.delete().uri("/api/products/" + createdProductId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        // Verify deleted
        webTestClient.get().uri("/api/products/" + createdProductId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(16)
    void getProduct_notFound_returns404() {
        webTestClient.get().uri("/api/products/999999")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ──────────────────────────────────────────────
    // 3. Document (PZ) creation
    // ──────────────────────────────────────────────

    @Test
    @Order(20)
    void createPZDocument_success() {
        // First create a product for the document
        String prodJson = """
                {"name": "Doc Product", "sku": "API-DOC-001", "unit": "szt.", "quantity": 50}
                """;

        byte[] prodBody = webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(prodJson)
                .exchange()
                .expectStatus().isCreated()
                .returnResult()
                .getResponseBodyContent();

        Long productId = extractJsonLong(prodBody, "id");
        assertNotNull(productId);

        // Create a contractor for the document
        String contractorJson = """
                {"name": "Test Supplier", "taxId": "1234567890", "type": "SUPPLIER", "active": true}
                """;

        byte[] contractorBody = webTestClient.post().uri("/api/contractors")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(contractorJson)
                .exchange()
                .expectStatus().isCreated()
                .returnResult()
                .getResponseBodyContent();

        Long contractorId = extractJsonLong(contractorBody, "id");
        assertNotNull(contractorId);

        // Create PZ document
        String docJson = String.format("""
                {"type": "PZ", "contractorId": %d, "items": [{"productId": %d, "quantity": 10, "unitPrice": 10.00}]}
                """, contractorId, productId);

        byte[] docBody = webTestClient.post().uri("/api/documents")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(docJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.type").isEqualTo("PZ")
                .jsonPath("$.status").isEqualTo("DRAFT")
                .returnResult()
                .getResponseBodyContent();

        Long docId = extractJsonLong(docBody, "id");

        // Confirm document
        webTestClient.post().uri("/api/documents/" + docId + "/confirm")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("CONFIRMED");
    }

    @Test
    @Order(21)
    void createDocument_withoutItems_returns400() {
        webTestClient.post().uri("/api/documents")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"type": "PZ", "items": []}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ──────────────────────────────────────────────
    // 4. Tenant isolation (verified via different tenant tokens)
    // ──────────────────────────────────────────────

    @Test
    @Order(30)
    void tenantIsolation_otherTenantCannotSeeProducts() {
        // Admin (tenantId=1) creates a product
        String prodJson = """
                {"name": "Tenant 1 Product", "sku": "TENANT-ISO-001", "unit": "szt."}
                """;

        webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(prodJson)
                .exchange()
                .expectStatus().isCreated();

        // A user from tenantId=2 should not see this product
        String otherToken = jwtUtil.generateToken("other", "ROLE_ADMIN", 2L);

        webTestClient.get().uri("/api/products")
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();

        // Also verify the specific product is not accessible
        webTestClient.get().uri("/api/products?sku=TENANT-ISO-001")
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(0);
    }

    @Test
    @Order(31)
    void tenantIsolation_otherTenantCannotAccessById() {
        String otherToken = jwtUtil.generateToken("other", "ROLE_ADMIN", 2L);

        // Should get 404 when accessing product from different tenant
        webTestClient.get().uri("/api/products/1")
                .header("Authorization", "Bearer " + otherToken)
                .exchange()
                .expectStatus().isNotFound();
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

    private Long extractJsonLong(byte[] body, String key) {
        if (body == null) return null;
        String text = new String(body, java.nio.charset.StandardCharsets.UTF_8);
        String search = "\"" + key + "\":";
        int start = text.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        // Read until comma, bracket, or whitespace
        int end = start;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        return end > start ? Long.parseLong(text.substring(start, end)) : null;
    }
}
