package com.playvora.playvora_api.common.config;

import com.playvora.playvora_api.component.WebSocketAuthInterceptor;
import com.playvora.playvora_api.component.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
        // Enable a simple in‑memory message broker for destinations prefixed with "/topic" and "/queue".
        //
        // Heartbeats:
        // - The two values in setHeartbeatValue(...) are [server→client, client→server] in milliseconds.
        // - Here we configure heartbeats every 10 seconds in both directions.
        // - Heartbeats are only sent while there is an active WebSocket/STOMP connection and at least
        //   one subscription; they are used by the broker and clients to detect dead connections.
        // - This configuration does NOT itself log heartbeat frames. To see them on the server side,
        //   enable DEBUG/TRACE logging for Spring’s WebSocket/STOMP packages. On the client side
        //   (e.g. in draft-selection.html) you can see them via the STOMP debug logger.
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(webSocketTaskScheduler())
                .setHeartbeatValue(new long[]{10_000L, 10_000L});

        // Set the application destination prefix to "/app"
        config.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }

    @Bean
    @Primary
    public TaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
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
