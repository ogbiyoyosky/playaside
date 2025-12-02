package com.playvora.playvora_api.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String TOKEN_PARAM = "token";
    private static final String AUTH_HEADER_PARAM = "auth";
    
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // Extract token from query parameters
            String token = httpRequest.getParameter(TOKEN_PARAM);
            
            // If token is not in query params, try auth parameter
            if (token == null || token.isEmpty()) {
                token = httpRequest.getParameter(AUTH_HEADER_PARAM);
            }
            
            // If token is provided, store it in session attributes for later use
            if (token != null && !token.isEmpty()) {
                attributes.put("token", token);
                log.debug("Token extracted from query parameters and stored in session attributes");
            } else {
                log.debug("No token found in query parameters");
            }
        }
        
        return true; // Allow handshake to proceed
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               @org.springframework.lang.Nullable Exception exception) {
        // No action needed after handshake
        if (exception != null) {
            log.error("Error during WebSocket handshake: {}", exception.getMessage());
        }
    }
}

