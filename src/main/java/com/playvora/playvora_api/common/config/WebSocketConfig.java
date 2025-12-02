package com.playvora.playvora_api.common.config;

import com.playvora.playvora_api.component.WebSocketAuthInterceptor;
import com.playvora.playvora_api.component.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    // Create handshake interceptor instance
    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor = new WebSocketHandshakeInterceptor();

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Enable a simple message broker for destinations prefixed with "/topic"
        config.enableSimpleBroker("/topic", "/queue");

        // Set the application destination prefix to "/app"
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Register the "/ws" endpoint for WebSocket connections
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins for development
                .addInterceptors(webSocketHandshakeInterceptor) // Add handshake interceptor to extract token from query params
                .withSockJS(); // Enable SockJS fallback options
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        // Add authentication interceptor for incoming messages
        registration.interceptors(webSocketAuthInterceptor);
    }
    
    @Override
    public void configureClientOutboundChannel(@NonNull ChannelRegistration registration) {
        // Add interceptor to track outbound messages (messages sent to subscribers)
        registration.interceptors(webSocketAuthInterceptor);
    }
}
