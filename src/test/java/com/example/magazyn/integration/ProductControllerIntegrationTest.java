package com.example.magazyn.integration;

import com.example.magazyn.dto.CreateProductRequest;
import com.example.magazyn.dto.UpdateProductRequest;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LocationRepository locationRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN");
        userToken = jwtUtil.generateToken("user", "ROLE_USER");
    }

    @Test
    void getAllProducts_returnsPage() {
        webTestClient.get().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.totalElements").isNumber();
    }

    @Test
    void createProduct_created() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Integration Product");
        request.setSku("INT-001");
        request.setUnit("szt.");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setMinQuantity(5);

        webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.name").isEqualTo("Integration Product")
                .jsonPath("$.sku").isEqualTo("INT-001")
                .jsonPath("$.price").isEqualTo(99.99)
                .jsonPath("$.minQuantity").isEqualTo(5);
    }

    @Test
    void createProduct_duplicateSku_returnsError() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Product One");
        request.setSku("DUP-SKU");
        request.setUnit("szt.");

        // Create first product
        webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isCreated();

        // Try to create second product with same SKU
        webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getProductById_found() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Findable Product");
        request.setSku("FND-001");
        request.setUnit("szt.");

        var result = webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .returnResult();

        Long productId = objectMapper.readTree(result.getResponseBodyContent()).get("id").asLong();

        webTestClient.get().uri("/api/products/{id}", productId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sku").isEqualTo("FND-001");
    }

    @Test
    void getProductById_notFound() {
        webTestClient.get().uri("/api/products/{id}", 99999L)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateProduct_updatesSuccessfully() throws Exception {
        CreateProductRequest createReq = new CreateProductRequest();
        createReq.setName("Original Name");
        createReq.setSku("UPD-001");
        createReq.setUnit("szt.");

        var createResult = webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(createReq))
                .exchange()
                .expectStatus().isCreated()
                .returnResult();

        Long productId = objectMapper.readTree(createResult.getResponseBodyContent()).get("id").asLong();

        UpdateProductRequest updateReq = new UpdateProductRequest();
        updateReq.setName("Updated Name");
        updateReq.setPrice(BigDecimal.valueOf(199.99));

        webTestClient.put().uri("/api/products/{id}", productId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(updateReq))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Updated Name")
                .jsonPath("$.price").isEqualTo(199.99)
                .jsonPath("$.sku").isEqualTo("UPD-001");
    }

    @Test
    void deleteProduct_deletesSuccessfully() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Deletable Product");
        request.setSku("DEL-001");
        request.setUnit("szt.");

        var deleteCreateResult = webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isCreated()
                .returnResult();

        Long productId = objectMapper.readTree(deleteCreateResult.getResponseBodyContent()).get("id").asLong();

        webTestClient.delete().uri("/api/products/{id}", productId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        // Verify it's gone
        webTestClient.get().uri("/api/products/{id}", productId)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createProduct_withoutAuth_returns401() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Unauthorized Product");
        request.setSku("UNAUTH-001");
        request.setUnit("szt.");

        webTestClient.post().uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void createProduct_withUserRole_returns403() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("User Product");
        request.setSku("USER-001");
        request.setUnit("szt.");

        webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void assignLocation_success() throws Exception {
        // Create a product
        CreateProductRequest productReq = new CreateProductRequest();
        productReq.setName("Locatable Product");
        productReq.setSku("LOC-001");
        productReq.setUnit("szt.");

        var locCreateResult = webTestClient.post().uri("/api/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(productReq))
                .exchange()
                .expectStatus().isCreated()
                .returnResult();

        Long productId = objectMapper.readTree(locCreateResult.getResponseBodyContent()).get("id").asLong();

        // Create a location
        Location location = Location.builder()
                .code("INT-WH")
                .name("Integration Warehouse")
                .type(LocationType.WAREHOUSE)
                .build();
        location = locationRepository.save(location);

        // Assign location to product
        String assignJson = "{\"locationId\": " + location.getId() + "}";

        webTestClient.patch().uri("/api/products/{id}/location", productId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(assignJson)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.locationId").isEqualTo(location.getId());
    }
}
