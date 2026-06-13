package com.example.magazyn.integration;

import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.util.JwtUtil;
import com.example.magazyn.config.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LocationIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LocationRepository locationRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(1L);
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        locationRepository.deleteAll();
        adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
        userToken = jwtUtil.generateToken("user", "ROLE_USER", 1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getAllLocations_returnsList() {
        Location wh = Location.builder()
                .code("WH-01").name("Warehouse 1").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh);

        Location rack = Location.builder()
                .code("RACK-01").name("Rack 1").type(LocationType.RACK).parentId(wh.getId()).build();
        locationRepository.save(rack);

        webTestClient.get().uri("/api/locations")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].code").isEqualTo("WH-01")
                .jsonPath("$[1].code").isEqualTo("RACK-01");
    }

    @Test
    void getLocationTree_returnsTreeStructure() {
        Location warehouse = Location.builder()
                .code("WH-01").name("Main Warehouse").type(LocationType.WAREHOUSE).build();
        warehouse = locationRepository.save(warehouse);

        Location rack = Location.builder()
                .code("RACK-A").name("Rack A").type(LocationType.RACK).parentId(warehouse.getId()).build();
        rack = locationRepository.save(rack);

        Location shelf = Location.builder()
                .code("SH-A1").name("Shelf A1").type(LocationType.SHELF).parentId(rack.getId()).build();
        locationRepository.save(shelf);

        webTestClient.get().uri("/api/locations/tree")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].code").isEqualTo("WH-01")
                .jsonPath("$[0].children[0].code").isEqualTo("RACK-A")
                .jsonPath("$[0].children[0].children[0].code").isEqualTo("SH-A1");
    }

    @Test
    void getLocationTree_withMultipleRoots() {
        Location wh1 = Location.builder()
                .code("WH-01").name("Warehouse 1").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh1);

        Location wh2 = Location.builder()
                .code("WH-02").name("Warehouse 2").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh2);

        webTestClient.get().uri("/api/locations/tree")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    void createLocation_created() {
        String locationJson = """
                {"code": "NEW-WH", "name": "New Warehouse", "type": "WAREHOUSE"}
                """;

        webTestClient.post().uri("/api/locations")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(locationJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.code").isEqualTo("NEW-WH")
                .jsonPath("$.name").isEqualTo("New Warehouse")
                .jsonPath("$.type").isEqualTo("WAREHOUSE");
    }

    @Test
    void createLocation_withParent_created() {
        Location parent = Location.builder()
                .code("PARENT").name("Parent").type(LocationType.WAREHOUSE).build();
        parent = locationRepository.save(parent);

        String childJson = "{\"code\": \"CHILD-RACK\", \"name\": \"Child Rack\", \"type\": \"RACK\", \"parentId\": " + parent.getId() + "}";

        webTestClient.post().uri("/api/locations")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(childJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.parentId").isEqualTo(parent.getId());
    }

    @Test
    void createLocation_withInvalidParent_returnsError() {
        String locationJson = """
                {"code": "BAD-RACK", "name": "Bad Rack", "type": "RACK", "parentId": 99999}
                """;

        webTestClient.post().uri("/api/locations")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(locationJson)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getProductsByLocation_emptyList() {
        Location location = Location.builder()
                .code("EMPTY-LOC").name("Empty Location").type(LocationType.WAREHOUSE).build();
        location = locationRepository.save(location);

        webTestClient.get().uri("/api/locations/{id}/products", location.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void deleteLocation_withoutChildren() {
        Location location = Location.builder()
                .code("DEL-01").name("Deletable").type(LocationType.WAREHOUSE).build();
        location = locationRepository.save(location);

        webTestClient.delete().uri("/api/locations/{id}", location.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNoContent();

        // Verify it's gone
        webTestClient.get().uri("/api/locations/{id}", location.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteLocation_withChildren_returnsError() {
        Location parent = Location.builder()
                .code("PARENT").name("Parent").type(LocationType.WAREHOUSE).build();
        parent = locationRepository.save(parent);

        Location child = Location.builder()
                .code("CHILD").name("Child").type(LocationType.RACK).parentId(parent.getId()).build();
        locationRepository.save(child);

        webTestClient.delete().uri("/api/locations/{id}", parent.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createLocation_withUserRole_returns403() {
        String locationJson = """
                {"code": "USER-LOC", "name": "User Location", "type": "WAREHOUSE"}
                """;

        webTestClient.post().uri("/api/locations")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(locationJson)
                .exchange()
                .expectStatus().isForbidden();
    }
}
