package com.example.magazyn.integration;

import com.example.magazyn.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void register_success() throws Exception {
        String registerJson = """
                {
                    "username": "newuser",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(notNullValue())))
                .andExpect(jsonPath("$.username", is("newuser")))
                .andExpect(jsonPath("$.role", is("ROLE_USER")));
    }

    @Test
    void register_duplicateUsername_returnsError() throws Exception {
        String registerJson = """
                {
                    "username": "duplicateuser",
                    "password": "password123"
                }
                """;

        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk());

        // Second registration with same username
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void register_withoutAuth_returns401() throws Exception {
        String registerJson = """
                {
                    "username": "unauthuser",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_withInvalidData_returns400() throws Exception {
        String invalidJson = """
                {
                    "username": "ab",
                    "password": "12"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_success() throws Exception {
        // First register a user
        String registerJson = """
                {
                    "username": "logintest",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("admin", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk());

        // Then login
        String loginJson = """
                {
                    "username": "logintest",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", is(notNullValue())))
                .andExpect(jsonPath("$.username", is("logintest")))
                .andExpect(jsonPath("$.role", is("ROLE_USER")));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        String loginJson = """
                {
                    "username": "nonexistent",
                    "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessProtectedEndpoint_withValidToken_success() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("testuser", "ROLE_USER")))
                .andExpect(status().isOk());
    }

    @Test
    void accessProtectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessAdminEndpoint_withUserRole_returns403() throws Exception {
        // Create product requires ADMIN role
        String productJson = """
                {
                    "name": "Test",
                    "sku": "TST-AUTH",
                    "unit": "szt."
                }
                """;

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("regularuser", "ROLE_USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void accessAdminEndpoint_withAdminToken_success() throws Exception {
        String productJson = """
                {
                    "name": "Admin Product",
                    "sku": "ADM-001",
                    "unit": "szt."
                }
                """;

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + jwtUtil.generateToken("adminuser", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson))
                .andExpect(status().isCreated());
    }

    @Test
    void accessActuatorHealth_withoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
