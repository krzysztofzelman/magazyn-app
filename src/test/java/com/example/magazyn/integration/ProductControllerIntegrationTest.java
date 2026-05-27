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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LocationRepository locationRepository;

    private HttpHeaders adminHeaders;
    private HttpHeaders userHeaders;

    @BeforeEach
    void setUp() {
        adminHeaders = new HttpHeaders();
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        adminHeaders.setBearerAuth(jwtUtil.generateToken("admin", "ROLE_ADMIN"));

        userHeaders = new HttpHeaders();
        userHeaders.setContentType(MediaType.APPLICATION_JSON);
        userHeaders.setBearerAuth(jwtUtil.generateToken("user", "ROLE_USER"));
    }

    @Test
    void getAllProducts_returnsPage() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("content", "totalElements");
    }

    @Test
    void createProduct_created() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Integration Product");
        request.setSku("INT-001");
        request.setUnit("szt.");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setMinQuantity(5);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), adminHeaders),
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var json = objectMapper.readTree(response.getBody());
        assertThat(json.get("id")).isNotNull();
        assertThat(json.get("name").asText()).isEqualTo("Integration Product");
        assertThat(json.get("sku").asText()).isEqualTo("INT-001");
        assertThat(json.get("price").asDecimal()).isEqualByComparingTo("99.99");
        assertThat(json.get("minQuantity").asInt()).isEqualTo(5);
    }

    @Test
    void createProduct_duplicateSku_returnsError() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Product One");
        request.setSku("DUP-SKU");
        request.setUnit("szt.");

        // Create first product
        ResponseEntity<String> first = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), adminHeaders),
                String.class);
        assertThat(first.getStatusCode().value()).isEqualTo(201);

        // Try to create second product with same SKU
        ResponseEntity<String> second = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), adminHeaders),
                String.class);
        assertThat(second.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void getProductById_found() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Findable Product");
        request.setSku("FND-001");
        request.setUnit("szt.");

        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), adminHeaders),
                String.class);
        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);

        Long productId = objectMapper.readTree(createResponse.getBody()).get("id").asLong();

        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/products/" + productId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(getResponse.getStatusCode().value()).isEqualTo(200);
        var json = objectMapper.readTree(getResponse.getBody());
        assertThat(json.get("sku").asText()).isEqualTo("FND-001");
    }

    @Test
    void getProductById_notFound() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products/99999", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void updateProduct_updatesSuccessfully() throws Exception {
        CreateProductRequest createReq = new CreateProductRequest();
        createReq.setName("Original Name");
        createReq.setSku("UPD-001");
        createReq.setUnit("szt.");

        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(createReq), adminHeaders),
                String.class);
        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);

        Long productId = objectMapper.readTree(createResponse.getBody()).get("id").asLong();

        UpdateProductRequest updateReq = new UpdateProductRequest();
        updateReq.setName("Updated Name");
        updateReq.setPrice(BigDecimal.valueOf(199.99));

        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/api/products/" + productId, HttpMethod.PUT,
                new HttpEntity<>(objectMapper.writeValueAsString(updateReq), adminHeaders),
                String.class);

        assertThat(updateResponse.getStatusCode().value()).isEqualTo(200);
        var json = objectMapper.readTree(updateResponse.getBody());
        assertThat(json.get("name").asText()).isEqualTo("Updated Name");
        assertThat(json.get("price").asDecimal()).isEqualByComparingTo("199.99");
        assertThat(json.get("sku").asText()).isEqualTo("UPD-001");
    }

    @Test
    void deleteProduct_deletesSuccessfully() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Deletable Product");
        request.setSku("DEL-001");
        request.setUnit("szt.");

        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), adminHeaders),
                String.class);
        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);

        Long productId = objectMapper.readTree(createResponse.getBody()).get("id").asLong();

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/products/" + productId, HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders), String.class);
        assertThat(deleteResponse.getStatusCode().value()).isEqualTo(204);

        // Verify it's gone
        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/products/" + productId, HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);
        assertThat(getResponse.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void createProduct_withoutAuth_returns401() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Unauthorized Product");
        request.setSku("UNAUTH-001");
        request.setUnit("szt.");

        HttpHeaders noAuthHeaders = new HttpHeaders();
        noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), noAuthHeaders),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void createProduct_withUserRole_returns403() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("User Product");
        request.setSku("USER-001");
        request.setUnit("szt.");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(request), userHeaders),
                String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void assignLocation_success() throws Exception {
        // Create a product
        CreateProductRequest productReq = new CreateProductRequest();
        productReq.setName("Locatable Product");
        productReq.setSku("LOC-001");
        productReq.setUnit("szt.");

        ResponseEntity<String> createResponse = restTemplate.exchange(
                "/api/products", HttpMethod.POST,
                new HttpEntity<>(objectMapper.writeValueAsString(productReq), adminHeaders),
                String.class);
        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
        Long productId = objectMapper.readTree(createResponse.getBody()).get("id").asLong();

        // Create a location
        Location location = Location.builder()
                .code("INT-WH")
                .name("Integration Warehouse")
                .type(LocationType.WAREHOUSE)
                .build();
        location = locationRepository.save(location);

        // Assign location to product
        String assignJson = "{\"locationId\": " + location.getId() + "}";

        ResponseEntity<String> assignResponse = restTemplate.exchange(
                "/api/products/" + productId + "/location", HttpMethod.PATCH,
                new HttpEntity<>(assignJson, adminHeaders),
                String.class);

        assertThat(assignResponse.getStatusCode().value()).isEqualTo(200);
        var json = objectMapper.readTree(assignResponse.getBody());
        assertThat(json.get("locationId").asInt()).isEqualTo(location.getId().intValue());
    }
}
