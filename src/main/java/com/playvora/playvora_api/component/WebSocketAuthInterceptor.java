package com.playvora.playvora_api.component;

import com.playvora.playvora_api.user.services.IJwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final IJwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            // Handle CONNECT command - initial authentication
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                log.info("CONNECT message received - Session ID: {}", accessor.getSessionId());
                org.springframework.security.core.Authentication authentication = authenticateConnection(accessor);
                // Ensure authentication is set in SecurityContext for this thread
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    // Verify it was set
                    org.springframework.security.core.Authentication verifyAuth = 
                            SecurityContextHolder.getContext().getAuthentication();
                    if (verifyAuth != null) {
                        log.info("✓ Authentication set in SecurityContext for CONNECT (user: {}, session: {})", 
                                verifyAuth.getName(), accessor.getSessionId());
                    } else {
                        log.error("✗ Failed to set authentication in SecurityContext for CONNECT (session: {})", 
                                accessor.getSessionId());
                    }
                } else {
                    log.warn("✗ No authentication created for CONNECT (session: {})", accessor.getSessionId());
                }
            }
            // Handle SUBSCRIBE command - log subscription events
            else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                String sessionId = accessor.getSessionId();
                String destination = accessor.getDestination();
                String subscriptionId = accessor.getSubscriptionId();
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                
                // Get user information if available
                String userName = "anonymous";
                Object userObj = accessor.getUser();
                if (userObj instanceof org.springframework.security.core.Authentication) {
                    org.springframework.security.core.Authentication auth = 
                            (org.springframework.security.core.Authentication) userObj;
                    userName = auth.getName();
                } else if (sessionAttributes != null) {
                    Object authObj = sessionAttributes.get("authentication");
                    if (authObj instanceof org.springframework.security.core.Authentication) {
                        org.springframework.security.core.Authentication auth = 
                                (org.springframework.security.core.Authentication) authObj;
                        userName = auth.getName();
                    } else if (sessionAttributes.get("user") != null) {
                        userName = sessionAttributes.get("user").toString();
                    }
                }
                
                log.info("=== 📡 SUBSCRIBE Event ===");
                log.info("Session ID: {}", sessionId);
                log.info("User: {}", userName);
                log.info("Destination (Topic): {}", destination);
                log.info("Subscription ID: {}", subscriptionId);
                log.info("Session authenticated: {}", sessionAttributes != null && 
                        sessionAttributes.containsKey("authenticated") ? 
                        sessionAttributes.get("authenticated") : "unknown");
                
                // Log all headers for debugging
                if (accessor.toNativeHeaderMap() != null && !accessor.toNativeHeaderMap().isEmpty()) {
                    log.debug("Subscription headers: {}", accessor.toNativeHeaderMap());
                }
                
                log.info("✓ Client subscribed to topic: {} (Session: {}, User: {})", 
                        destination, sessionId, userName);
            }
            // Handle UNSUBSCRIBE command - log unsubscription events
            else if (StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())) {
                String sessionId = accessor.getSessionId();
                String subscriptionId = accessor.getSubscriptionId();
                String destination = accessor.getDestination();
                
                log.info("=== 📡 UNSUBSCRIBE Event ===");
                log.info("Session ID: {}", sessionId);
                log.info("Subscription ID: {}", subscriptionId);
                log.info("Destination (Topic): {}", destination);
                log.info("✓ Client unsubscribed from topic: {} (Session: {})", 
                        destination != null ? destination : "unknown", sessionId);
            }
            // Handle DISCONNECT command - log disconnection events
            else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                String sessionId = accessor.getSessionId();
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                
                String userName = "anonymous";
                if (sessionAttributes != null && sessionAttributes.get("user") != null) {
                    userName = sessionAttributes.get("user").toString();
                }
                
                log.info("=== 📡 DISCONNECT Event ===");
                log.info("Session ID: {}", sessionId);
                log.info("User: {}", userName);
                log.info("✓ Client disconnected (Session: {}, User: {})", sessionId, userName);
            }
            // Handle SEND command - ensure authentication is set in SecurityContext
            else if (StompCommand.SEND.equals(accessor.getCommand())) {
                String sessionId = accessor.getSessionId();
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                org.springframework.security.core.Authentication sessionAuth = null;
                
                log.info("SEND message received - Session ID: {}, Has session attributes: {}, Session attributes keys: {}", 
                        sessionId, 
                        sessionAttributes != null, 
                        sessionAttributes != null ? sessionAttributes.keySet() : "null");
                
                // Try multiple ways to get authentication
                // 1. Check if authentication is already in accessor (from CONNECT)
                Object userObj = accessor.getUser();
                if (userObj != null) {
                    log.info("Accessor.getUser() for SEND message: {}", userObj.getClass().getName());
                    if (userObj instanceof org.springframework.security.core.Authentication) {
                        sessionAuth = (org.springframework.security.core.Authentication) userObj;
                        log.info("✓ Authentication found in accessor for SEND message (user: {})", sessionAuth.getName());
                    } else {
                        log.warn("Accessor.getUser() is not an Authentication instance. Type: {}", userObj.getClass().getName());
                        // Try to cast anyway
                        try {
                            sessionAuth = org.springframework.security.core.Authentication.class.cast(userObj);
                            log.info("✓ Cast successful - Authentication found in accessor for SEND message (user: {})", 
                                    sessionAuth.getName());
                        } catch (ClassCastException e) {
                            log.warn("Cannot cast accessor.getUser() to Authentication: {}", e.getMessage());
                        }
                    }
                } else {
                    log.info("Accessor.getUser() is null for SEND message");
                }
                
                // 2. If not in accessor, check session attributes for stored authentication
                if (sessionAuth == null && sessionAttributes != null) {
                    Object authObj = sessionAttributes.get("authentication");
                    if (authObj != null) {
                        log.info("Found authentication object in session attributes, type: {}", authObj.getClass().getName());
                        if (authObj instanceof org.springframework.security.core.Authentication) {
                            sessionAuth = (org.springframework.security.core.Authentication) authObj;
                            log.info("Retrieved authentication from session attributes for SEND message (user: {})", 
                                    sessionAuth.getName());
                            // Update accessor with the authentication
                            accessor.setUser(sessionAuth);
                        } else {
                            log.warn("Authentication object in session is not of expected type: {}", authObj.getClass().getName());
                        }
                    } else {
                        log.info("No authentication object found in session attributes");
                    }
                }
                
                // 3. If still not found, try to reconstruct from stored token
                if (sessionAuth == null && sessionAttributes != null) {
                    Object tokenAttr = sessionAttributes.get("token");
                    if (tokenAttr != null && tokenAttr instanceof String) {
                        String jwt = (String) tokenAttr;
                        log.info("Reconstructing authentication from stored token for SEND message");
                        sessionAuth = createAuthenticationFromToken(jwt);
                        if (sessionAuth != null) {
                            // Store authentication in both accessor and session attributes
                            accessor.setUser(sessionAuth);
                            sessionAttributes.put("authentication", sessionAuth);
                            log.info("Reconstructed and stored authentication for SEND message (user: {})", 
                                    sessionAuth.getName());
                        } else {
                            log.warn("Failed to reconstruct authentication from token");
                        }
                    } else {
                        log.info("No token found in session attributes for SEND message");
                    }
                }
                
                // 4. If still not found, try to authenticate from headers (fallback)
                if (sessionAuth == null) {
                    log.info("No session authentication found for SEND message (session: {}), attempting to authenticate from token", 
                            sessionId);
                    sessionAuth = authenticateConnection(accessor);
                }
                
                // CRITICAL: Always set authentication in SecurityContext for this thread
                // SecurityContext is thread-local, so we need to set it for each message
                if (sessionAuth != null) {
                    SecurityContextHolder.getContext().setAuthentication(sessionAuth);
                    // Verify it was set
                    org.springframework.security.core.Authentication verifyAuth = 
                            SecurityContextHolder.getContext().getAuthentication();
                    if (verifyAuth != null) {
                        log.info("✓ Authentication set in SecurityContext for SEND message (user: {}, session: {})", 
                                verifyAuth.getName(), sessionId);
                    } else {
                        log.error("✗ Failed to set authentication in SecurityContext for SEND message (session: {})", 
                                sessionId);
                    }
                } else {
                    log.error("✗ No authentication available for SEND message (session: {})", sessionId);
                }
            }
        }
        
        return message;
    }
    
    /**
     * Creates an Authentication object from a JWT token
     */
    private org.springframework.security.core.Authentication createAuthenticationFromToken(String jwt) {
        try {
            String email = jwtService.extractUsername(jwt);
            if (email != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    return new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                    );
                } else {
                    log.warn("Invalid JWT token");
                }
            }
        } catch (Exception e) {
            log.error("Error creating authentication from token: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Authenticates a WebSocket connection using token from query parameters or headers.
     * Returns the Authentication object if successful, null otherwise.
     */
    private org.springframework.security.core.Authentication authenticateConnection(StompHeaderAccessor accessor) {
            String jwt = null;
        String sessionId = accessor.getSessionId();
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        
        log.debug("Authenticating WebSocket connection - Session ID: {}, Has session attributes: {}", 
                sessionId, sessionAttributes != null);
        
        // 1. First, check session attributes (token from query parameters during handshake)
        if (sessionAttributes != null) {
            Object tokenAttr = sessionAttributes.get("token");
            if (tokenAttr != null && tokenAttr instanceof String) {
                jwt = (String) tokenAttr;
                log.info("JWT extracted from session attributes (query parameter) for session: {}", sessionId);
            } else {
                log.debug("No token found in session attributes. Available keys: {}", 
                        sessionAttributes.keySet() != null ? sessionAttributes.keySet() : "null");
            }
        } else {
            log.debug("Session attributes are null for session: {}", sessionId);
        }
        
        // 2. Check for Authorization header (fallback)
        if (jwt == null || jwt.isEmpty()) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                log.debug("JWT extracted from Authorization header");
            }
            }
            
        // 3. Check for token in custom header (fallback)
        if (jwt == null || jwt.isEmpty()) {
                jwt = accessor.getFirstNativeHeader("token");
                if (jwt != null) {
                    log.debug("JWT extracted from token header");
                }
            }
            
        // 4. Check for auth-token header (fallback)
        if (jwt == null || jwt.isEmpty()) {
                String tokenParam = accessor.getFirstNativeHeader("auth-token");
                if (tokenParam != null) {
                    jwt = tokenParam;
                    log.debug("JWT extracted from auth-token header");
                }
            }
            
            // Validate and authenticate
        if (jwt != null && !jwt.isEmpty()) {
            try {
                org.springframework.security.core.Authentication authentication = createAuthenticationFromToken(jwt);
                if (authentication != null) {
                            // Set authentication in accessor for this WebSocket session
                            accessor.setUser(authentication);
                            
                    // Store token and authentication object in session attributes for subsequent messages
                    if (sessionAttributes != null) {
                        sessionAttributes.put("token", jwt);
                        sessionAttributes.put("authentication", authentication); // Store the authentication object
                        sessionAttributes.put("authenticated", true);
                        sessionAttributes.put("user", authentication.getName());
                        log.debug("Stored authentication in session attributes for session: {} (user: {})", 
                                sessionId, authentication.getName());
                        } else {
                        log.warn("Cannot store authentication in session attributes - sessionAttributes is null for session: {}", 
                                sessionId);
                    }
                    
                    log.info("WebSocket authenticated for user: {} (session: {})", authentication.getName(), sessionId);
                    return authentication;
                } else {
                    log.warn("Failed to create authentication from token for session: {}", sessionId);
                }
            } catch (Exception e) {
                log.error("Error authenticating WebSocket connection (session: {}): {}", sessionId, e.getMessage(), e);
            }
        } else {
            log.warn("No JWT token found in WebSocket connection (session: {})", sessionId);
        }
        
        return null;
    }
    
    @Override
    public void postSend(@NonNull Message<?> message, @NonNull MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && sent) {
            // Log when messages are sent to subscribers (MESSAGE command indicates outbound message)
            if (StompCommand.MESSAGE.equals(accessor.getCommand())) {
                String destination = accessor.getDestination();
                String subscriptionId = accessor.getSubscriptionId();
                String sessionId = accessor.getSessionId();
                
                log.debug("📤 Message sent to subscriber - Destination: {}, Subscription ID: {}, Session ID: {}", 
                        destination, subscriptionId, sessionId);
            }
        }
    }
}

