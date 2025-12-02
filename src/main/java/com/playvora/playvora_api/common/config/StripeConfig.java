package com.playvora.playvora_api.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class StripeConfig {
    
    @Value("${stripe.secret-key:}")
    private String secretKey;
    
    @Value("${stripe.publishable-key:}")
    private String publishableKey;
    
    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;
    
    private boolean stripeAvailable = true;
    
    @PostConstruct
    public void init() {
        try {
            // Try to initialize Stripe if available
            Class<?> stripeClass = Class.forName("com.stripe.Stripe");
            if (secretKey != null && !secretKey.isEmpty() && !secretKey.startsWith("sk_test_...")) {
                // Only initialize if we have a real key
                // Use reflection to set the API key to avoid compilation errors
                java.lang.reflect.Field apiKeyField = stripeClass.getDeclaredField("apiKey");
                apiKeyField.setAccessible(true);
                apiKeyField.set(null, secretKey);
                stripeAvailable = true;
                log.info("Stripe initialized successfully");
            } else {
                log.warn("Stripe not initialized - using placeholder keys");
            }
        } catch (ClassNotFoundException e) {
            log.warn("Stripe SDK not available - payment features will be disabled");
        } catch (Exception e) {
            log.error("Error initializing Stripe: {}", e.getMessage());
        }
    }
    
    public String getPublishableKey() {
        return publishableKey;
    }
    
    public String getWebhookSecret() {
        return webhookSecret;
    }
    
    public boolean isStripeAvailable() {
        return stripeAvailable;
    }
}
