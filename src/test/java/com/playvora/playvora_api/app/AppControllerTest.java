package com.playvora.playvora_api.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return welcome message at root endpoint")
    void testRootEndpoint() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Welcome to Playvora API"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should return welcome message at /api/v1 endpoint")
    void testApiV1Endpoint() throws Exception {
        mockMvc.perform(get("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Welcome to Playvora API"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Should allow public access to welcome endpoints without authentication")
    void testPublicAccessToWelcomeEndpoints() throws Exception {
        // Root endpoint should be accessible without authentication
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        // API v1 endpoint should be accessible without authentication
        mockMvc.perform(get("/api/v1"))
                .andExpect(status().isOk());
    }
}

