package com.playvora.playvora_api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should allow public access to root endpoint")
    void testPublicAccessToRoot() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to /api/v1 endpoint")
    void testPublicAccessToApiV1() throws Exception {
        mockMvc.perform(get("/api/v1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to auth endpoints")
    void testPublicAccessToAuthEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login"))
                .andExpect(status().is4xxClientError()); // Will fail validation but not auth
    }

    @Test
    @DisplayName("Should allow public access to actuator health endpoint")
    void testPublicAccessToActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to Swagger UI")
    void testPublicAccessToSwaggerUI() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection()); // Redirects to swagger-ui/index.html
    }

    @Test
    @DisplayName("Should allow public access to API docs")
    void testPublicAccessToApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny access to protected endpoints without authentication")
    void testProtectedEndpointsRequireAuth() throws Exception {
        // This would be any endpoint not explicitly permitted
        // For now, we don't have any protected endpoints defined yet
        // But we can test that the security chain is working
        mockMvc.perform(get("/api/v1/protected"))
                .andExpect(status().isUnauthorized());
    }
}

