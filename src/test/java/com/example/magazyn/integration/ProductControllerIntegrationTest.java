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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
        adminToken = "Bearer " + jwtUtil.generateToken("admin", "ROLE_ADMIN");
        userToken = "Bearer " + jwtUtil.generateToken("user", "ROLE_USER");
    }

    @Test
    void getAllProducts_returnsPage() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is(notNullValue())))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void createProduct_created() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Integration Product");
        request.setSku("INT-001");
        request.setUnit("szt.");
        request.setPrice(BigDecimal.valueOf(99.99));
        request.setMinQuantity(5);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.name", is("Integration Product")))
                .andExpect(jsonPath("$.sku", is("INT-001")))
                .andExpect(jsonPath("$.price", is(99.99)))
                .andExpect(jsonPath("$.minQuantity", is(5)));
    }

    @Test
    void createProduct_duplicateSku_returns400() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Product One");
        request.setSku("DUP-SKU");
        request.setUnit("szt.");

        // Create first product
        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to create second product with same SKU
        mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getProductById_found() throws Exception {
        // First create a product
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Findable Product");
        request.setSku("FND-001");
        request.setUnit("szt.");

        String createResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Parse the id from the response
        Long productId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then get it by id
        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku", is("FND-001")));
    }

    @Test
    void getProductById_notFound() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 99999L)
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_updatesSuccessfully() throws Exception {
        // Create product first
        CreateProductRequest createReq = new CreateProductRequest();
        createReq.setName("Original Name");
        createReq.setSku("UPD-001");
        createReq.setUnit("szt.");

        String createResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(createResponse).get("id").asLong();

        // Update the product
        UpdateProductRequest updateReq = new UpdateProductRequest();
        updateReq.setName("Updated Name");
        updateReq.setPrice(BigDecimal.valueOf(199.99));

        mockMvc.perform(put("/api/products/{id}", productId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.price", is(199.99)))
                .andExpect(jsonPath("$.sku", is("UPD-001"))); // unchanged
    }

    @Test
    void deleteProduct_deletesSuccessfully() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Deletable Product");
        request.setSku("DEL-001");
        request.setUnit("szt.");

        String createResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/products/{id}", productId)
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_withoutAuth_returns401() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Unauthorized Product");
        request.setSku("UNAUTH-001");
        request.setUnit("szt.");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_withUserRole_returns403() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("User Product");
        request.setSku("USER-001");
        request.setUnit("szt.");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignLocation_success() throws Exception {
        // Create a product
        CreateProductRequest productReq = new CreateProductRequest();
        productReq.setName("Locatable Product");
        productReq.setSku("LOC-001");
        productReq.setUnit("szt.");

        String createResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(createResponse).get("id").asLong();

        // Create a location
        Location location = Location.builder()
                .code("INT-WH")
                .name("Integration Warehouse")
                .type(LocationType.WAREHOUSE)
                .build();
        location = locationRepository.save(location);

        // Assign location to product
        String assignJson = "{\"locationId\": " + location.getId() + "}";

        mockMvc.perform(patch("/api/products/{id}/location", productId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId", is(location.getId().intValue())));
    }
}
