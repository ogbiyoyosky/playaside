package com.playvora.playvora_api.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Configuration to handle HTTPS when application is behind a reverse proxy (nginx/load balancer).
 * 
 * This ensures that:
 * - OAuth2 redirect URIs use HTTPS
 * - Swagger UI uses HTTPS for all endpoints
 * - Spring Security recognizes the original HTTPS protocol
 */
@Configuration
public class HttpsConfig {

    /**
     * Processes X-Forwarded-* headers from the proxy to detect HTTPS.
     * 
     * When nginx/load balancer sends:
     * X-Forwarded-Proto: https
     * X-Forwarded-Host: devapi.playaside.com
     * 
     * This filter ensures Spring Boot knows the original request was HTTPS
     * and generates all URLs with https:// instead of http://
     */
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}

