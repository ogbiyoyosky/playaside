package com.playvora.playvora_api.component;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicCorsConfigurationSource implements CorsConfigurationSource {


    // You can cache the CorsConfiguration for a short period if fetching domains frequently is a performance concern.
    // For simplicity, this example reconstructs it on each call or relies on CorsFilter's caching.

    @Override
    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(getAllAllowedOrigins()); // Dynamically fetch origins
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Origin", "Content-Type", "Accept", "Accept-Encoding", "Authorization",
                "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
                "X-Requested-With", "X-Page-Title", "X-Screen-Width", "X-Screen-Height",
                "X-Initial-Referring-Domain", "X-Initial-Referrer", "X-Auth-Token",
                "X-User-Role-Id",
                "Accept-Language", "X-Locale"
        ));
        config.setExposedHeaders(Collections.singletonList("X-Auth-Token"));
        config.setMaxAge(3600L);

        // Log the allowed origins for debugging for this specific request if needed
        // log.debug("Dynamically configured CORS allowed origins for request {}: {}", request.getRequestURI(), config.getAllowedOrigins());

        return config;
    }

    private List<String> getAllAllowedOrigins() {
        List<String> allowedOrigins = new ArrayList<>();

        // Static origins
        allowedOrigins.add("http://localhost:8082");
        allowedOrigins.add("https://playaside.com");
        allowedOrigins.add("https://www.playaside.com");
        allowedOrigins.add("https://dev.playaside.com");
        allowedOrigins.add("https://app.playaside.com");
        allowedOrigins.add("https://devapp.playaside.com");
        allowedOrigins.add("https://devapi.playaside.com");
        allowedOrigins.add("https://api.playaside.com");
        allowedOrigins.add("https://dev.app.playaside.com");
        allowedOrigins.add("https://preview.app.playaside.com");
        allowedOrigins.add("https://app.playaside.com");
        allowedOrigins.add("https://devapp.playaside.com:3000");
        allowedOrigins.add("http://192.168.178.139:8082");
        allowedOrigins.add("https://devapp.playaside.com:8082"); // Specific port for dev

        // Add local development origins with both http and https
        addLocalOrigins(allowedOrigins);

        // // Add fly.io origins
        // allowedOrigins.add("https://playaside-api.fly.dev");

        // Add domain origins from repository (dynamic part)
        try {

            if (log.isDebugEnabled()) { // More detailed logging for debug
                //  log.debug("Loaded dynamic domain origins: {}", domainOrigins);
            }

        } catch (Exception e) {
            log.error("Error loading dynamic domain origins for CORS: {}", e.getMessage(), e);
        }
        
        // log.info("Final compiled list of allowed CORS origins: {}", allowedOrigins.size());
        return allowedOrigins;
    }

    private void addLocalOrigins(List<String> allowedOrigins) {
        for (int port = 3000; port <= 3009; port++) {
            allowedOrigins.add(String.format("http://localhost:%d", port));
            allowedOrigins.add(String.format("https://localhost:%d", port));
        }
        allowedOrigins.add("http://localhost:8080"); 
        allowedOrigins.add("http://localhost:8082");
        allowedOrigins.add("http://192.168.178.139:8082");
        // For local API testing if served on 8080
    }
} 