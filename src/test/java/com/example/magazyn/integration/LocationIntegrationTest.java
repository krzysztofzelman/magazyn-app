package com.example.magazyn.integration;

import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LocationRepository locationRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        locationRepository.deleteAll();
        adminToken = "Bearer " + jwtUtil.generateToken("admin", "ROLE_ADMIN");
        userToken = "Bearer " + jwtUtil.generateToken("user", "ROLE_USER");
    }

    @Test
    void getAllLocations_returnsList() throws Exception {
        // Create locations
        Location wh = Location.builder()
                .code("WH-01").name("Warehouse 1").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh);

        Location rack = Location.builder()
                .code("RACK-01").name("Rack 1").type(LocationType.RACK).parentId(wh.getId()).build();
        locationRepository.save(rack);

        mockMvc.perform(get("/api/locations")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].code", is("WH-01")))
                .andExpect(jsonPath("$[1].code", is("RACK-01")));
    }

    @Test
    void getLocationTree_returnsTreeStructure() throws Exception {
        // Create location hierarchy
        Location warehouse = Location.builder()
                .code("WH-01").name("Main Warehouse").type(LocationType.WAREHOUSE).build();
        warehouse = locationRepository.save(warehouse);

        Location rack = Location.builder()
                .code("RACK-A").name("Rack A").type(LocationType.RACK).parentId(warehouse.getId()).build();
        rack = locationRepository.save(rack);

        Location shelf = Location.builder()
                .code("SH-A1").name("Shelf A1").type(LocationType.SHELF).parentId(rack.getId()).build();
        locationRepository.save(shelf);

        mockMvc.perform(get("/api/locations/tree")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("WH-01")))
                .andExpect(jsonPath("$[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].code", is("RACK-A")))
                .andExpect(jsonPath("$[0].children[0].children", hasSize(1)))
                .andExpect(jsonPath("$[0].children[0].children[0].code", is("SH-A1")));
    }

    @Test
    void getLocationTree_withMultipleRoots() throws Exception {
        Location wh1 = Location.builder()
                .code("WH-01").name("Warehouse 1").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh1);

        Location wh2 = Location.builder()
                .code("WH-02").name("Warehouse 2").type(LocationType.WAREHOUSE).build();
        locationRepository.save(wh2);

        mockMvc.perform(get("/api/locations/tree")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void createLocation_created() throws Exception {
        String locationJson = """
                {
                    "code": "NEW-WH",
                    "name": "New Warehouse",
                    "type": "WAREHOUSE"
                }
                """;

        mockMvc.perform(post("/api/locations")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(notNullValue())))
                .andExpect(jsonPath("$.code", is("NEW-WH")))
                .andExpect(jsonPath("$.name", is("New Warehouse")))
                .andExpect(jsonPath("$.type", is("WAREHOUSE")));
    }

    @Test
    void createLocation_withParent_created() throws Exception {
        // Create parent location first
        Location parent = Location.builder()
                .code("PARENT").name("Parent").type(LocationType.WAREHOUSE).build();
        parent = locationRepository.save(parent);

        String childJson = String.format("""
                {
                    "code": "CHILD-RACK",
                    "name": "Child Rack",
                    "type": "RACK",
                    "parentId": %d
                }
                """, parent.getId());

        mockMvc.perform(post("/api/locations")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(childJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId", is(parent.getId().intValue())));
    }

    @Test
    void createLocation_withInvalidParent_returnsError() throws Exception {
        String locationJson = """
                {
                    "code": "BAD-RACK",
                    "name": "Bad Rack",
                    "type": "RACK",
                    "parentId": 99999
                }
                """;

        mockMvc.perform(post("/api/locations")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void getProductsByLocation_emptyList() throws Exception {
        Location location = Location.builder()
                .code("EMPTY-LOC").name("Empty Location").type(LocationType.WAREHOUSE).build();
        location = locationRepository.save(location);

        mockMvc.perform(get("/api/locations/{id}/products", location.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(empty())));
    }

    @Test
    void deleteLocation_withoutChildren() throws Exception {
        Location location = Location.builder()
                .code("DEL-01").name("Deletable").type(LocationType.WAREHOUSE).build();
        location = locationRepository.save(location);

        mockMvc.perform(delete("/api/locations/{id}", location.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/locations/{id}", location.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLocation_withChildren_returnsError() throws Exception {
        Location parent = Location.builder()
                .code("PARENT").name("Parent").type(LocationType.WAREHOUSE).build();
        parent = locationRepository.save(parent);

        Location child = Location.builder()
                .code("CHILD").name("Child").type(LocationType.RACK).parentId(parent.getId()).build();
        locationRepository.save(child);

        mockMvc.perform(delete("/api/locations/{id}", parent.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void createLocation_withUserRole_returns403() throws Exception {
        String locationJson = """
                {
                    "code": "USER-LOC",
                    "name": "User Location",
                    "type": "WAREHOUSE"
                }
                """;

        mockMvc.perform(post("/api/locations")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(locationJson))
                .andExpect(status().isForbidden());
    }
}
