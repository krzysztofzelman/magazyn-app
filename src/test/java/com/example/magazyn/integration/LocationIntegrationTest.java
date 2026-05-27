package com.example.magazyn.integration;

import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.util.JwtUtil;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LocationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LocationRepository locationRepository;

    private HttpHeaders adminHeaders;
    private HttpHeaders userHeaders;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        adminHeaders = new HttpHeaders();
        adminHeaders.setContentType(MediaType.APPLICATION_JSON);
        adminHeaders.setBearerAuth(jwtUtil.generateToken("admin", "ROLE_ADMIN"));

        userHeaders = new HttpHeaders();
        userHeaders.setContentType(MediaType.APPLICATION_JSON);
        userHeaders.setBearerAuth(jwtUtil.generateToken("user", "ROLE_USER"));
    }

    @Test
    void getAllLocations_returnsList() {
        Location wh = Location.builder()
                .code("WH-01").name("Warehouse 1").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh);

        Location rack = Location.builder()
                .code("RACK-01").name("Rack 1").type(LocationType.RACK).parentId(wh.getId()).build();
        locationRepository.save(rack);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("WH-01", "RACK-01");
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

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations/tree", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("WH-01", "RACK-A", "SH-A1");
    }

    @Test
    void getLocationTree_withMultipleRoots() {
        Location wh1 = Location.builder()
                .code("WH-01").name("Warehouse 1").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh1);

        Location wh2 = Location.builder()
                .code("WH-02").name("Warehouse 2").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh2);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations/tree", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("WH-01", "WH-02");
    }

    @Test
    void createLocation_created() throws Exception {
        String locationJson = """
                {"code": "NEW-WH", "name": "New Warehouse", "type": "WAREHOUSE"}
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations", HttpMethod.POST,
                new HttpEntity<>(locationJson, adminHeaders), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getBody());
        assertThat(json.get("id")).isNotNull();
        assertThat(json.get("code").asText()).isEqualTo("NEW-WH");
        assertThat(json.get("name").asText()).isEqualTo("New Warehouse");
        assertThat(json.get("type").asText()).isEqualTo("WAREHOUSE");
    }

    @Test
    void createLocation_withParent_created() {
        Location parent = Location.builder()
                .code("PARENT").name("Parent").type(LocationType.WAREHOUSE).build();
        parent = locationRepository.save(parent);

        String childJson = "{\"code\": \"CHILD-RACK\", \"name\": \"Child Rack\", \"type\": \"RACK\", \"parentId\": " + parent.getId() + "}";

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations", HttpMethod.POST,
                new HttpEntity<>(childJson, adminHeaders), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).contains("\"parentId\":");
    }

    @Test
    void createLocation_withInvalidParent_returnsError() {
        String locationJson = """
                {"code": "BAD-RACK", "name": "Bad Rack", "type": "RACK", "parentId": 99999}
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations", HttpMethod.POST,
                new HttpEntity<>(locationJson, adminHeaders), String.class);

        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void getProductsByLocation_emptyList() {
        Location location = Location.builder()
                .code("EMPTY-LOC").name("Empty Location").type(LocationType.WAREHOUSE).build();
        location = locationRepository.save(location);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations/" + location.getId() + "/products", HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("[]");
    }

    @Test
    void deleteLocation_withoutChildren() {
        Location location = Location.builder()
                .code("DEL-01").name("Deletable").type(LocationType.WAREHOUSE).build();
        location = locationRepository.save(location);

        ResponseEntity<String> deleteResponse = restTemplate.exchange(
                "/api/locations/" + location.getId(), HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders), String.class);
        assertThat(deleteResponse.getStatusCode().value()).isEqualTo(204);

        // Verify it's gone
        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/api/locations/" + location.getId(), HttpMethod.GET,
                new HttpEntity<>(adminHeaders), String.class);
        assertThat(getResponse.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void deleteLocation_withChildren_returnsError() {
        Location parent = Location.builder()
                .code("PARENT").name("Parent").type(LocationType.WAREHOUSE).build();
        parent = locationRepository.save(parent);

        Location child = Location.builder()
                .code("CHILD").name("Child").type(LocationType.RACK).parentId(parent.getId()).build();
        locationRepository.save(child);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations/" + parent.getId(), HttpMethod.DELETE,
                new HttpEntity<>(adminHeaders), String.class);
        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void createLocation_withUserRole_returns403() {
        String locationJson = """
                {"code": "USER-LOC", "name": "User Location", "type": "WAREHOUSE"}
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/locations", HttpMethod.POST,
                new HttpEntity<>(locationJson, userHeaders), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }
}
