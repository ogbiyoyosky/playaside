package com.playvora.playvora_api.match.controllers;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.match.dtos.chat.ChatMessageRequest;
import com.playvora.playvora_api.match.dtos.chat.ChatMessageResponse;
import com.playvora.playvora_api.match.dtos.websocket.MatchUpdateMessage;
import com.playvora.playvora_api.match.dtos.websocket.PlayerSelectionRequest;
import com.playvora.playvora_api.match.dtos.websocket.TeamSelectionMessage;
import com.playvora.playvora_api.match.dtos.websocket.WebSocketMessage;
import com.playvora.playvora_api.match.entities.ChatMessage;
import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.match.mappers.MatchEventMapper;
import com.playvora.playvora_api.match.repo.AvailabilityRepository;
import com.playvora.playvora_api.match.repo.ChatMessageRepository;
import com.playvora.playvora_api.match.repo.MatchRepository;
import com.playvora.playvora_api.match.repo.TeamRepository;
import com.playvora.playvora_api.match.services.IMatchService;
import com.playvora.playvora_api.notification.services.IPushNotificationService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket", description = "Real-time WebSocket messaging for team selection")
public class WebSocketController {
    
    private final IMatchService matchService;
    private final UserRepository userRepository;
    private final AvailabilityRepository availabilityRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final IPushNotificationService pushNotificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @MessageMapping("/match-events/{matchId}/select-player")
    @Operation(summary = "Select player for team", description = "Real-time player selection for team")
    public void selectPlayer(@DestinationVariable UUID matchId, 
                           @Payload @Valid PlayerSelectionRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        try {

            User currentUser = getCurrentUser(headerAccessor);
            log.info("User {} selecting player {} for team {} in match {}", 
                    currentUser.getId(), request.getUserId(), request.getTeamId(), matchId);
            
            // Double-check SecurityContext is set (should be set by getCurrentUser)
            org.springframework.security.core.Authentication authCheck = 
                    SecurityContextHolder.getContext().getAuthentication();
            if (authCheck == null) {
                log.error("✗✗✗ SecurityContext is NULL after getCurrentUser! Setting it now...");
                // Try to get authentication from headerAccessor and set it
                if (headerAccessor != null && headerAccessor.getUser() != null) {
                    Object userObj = headerAccessor.getUser();
                    if (userObj instanceof org.springframework.security.core.Authentication) {
                        SecurityContextHolder.getContext().setAuthentication(
                                (org.springframework.security.core.Authentication) userObj);
                        log.info("✓ SecurityContext set from headerAccessor as fallback");
                    }
                }
            } else {
                log.info("✓ SecurityContext verified - Authentication exists (user: {})", authCheck.getName());
            }
            
            // Validate request
            if (!request.getMatchId().equals(matchId)) {
                throw new BadRequestException("Match ID mismatch");
            }
            
            matchService.selectPlayerForTeam(matchId, request.getTeamId(), request.getUserId());
            
            // Get updated match data with all relationships loaded
            Match match = loadMatchWithRelationships(matchId);
            
            var availablePlayers = availabilityRepository.findAvailablePlayers(matchId);
            var matchResponse = MatchEventMapper.convertToResponse(match, availablePlayers);
            var user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BadRequestException("User not found"));
            
            // Create and send update message
            TeamSelectionMessage message = TeamSelectionMessage.builder()
                    .action("SELECT_PLAYER")
                    .matchId(matchId)
                    .teamId(request.getTeamId())
                    .userId(request.getUserId())
                    .userName(user.getFirstName() + " " + user.getLastName())
                    .teamName("Team " + request.getTeamId())
                    .message(user.getFirstName() + " " + user.getLastName() + " has been selected for the team")
                    .data(matchResponse)
                    .build();
            
            // Broadcast to all subscribers of this match
            String topic = "/topic/match/" + matchId + "/updates";
            WebSocketMessage wsMessage = WebSocketMessage.create("team_selection", message);
            
            
            // Send the message
            try {
        
                
                // Convert and send the message
                messagingTemplate.convertAndSend(topic, wsMessage);
                
                // Verify the message was serialized correctly
                try {
                    String jsonMessage = objectMapper.writeValueAsString(wsMessage);
                    log.info("Message JSON (first 500 chars): {}", 
                            jsonMessage.length() > 500 ? jsonMessage.substring(0, 500) + "..." : jsonMessage);
                } catch (Exception jsonError) {
                    log.warn("Could not serialize message to JSON for logging: {}", jsonError.getMessage());
                }
            } catch (Exception e) {
                log.error("✗ Error broadcasting match update: {}", e.getMessage(), e);
                log.error("Error stack trace:", e);
                throw e;
            }
            
            // Send confirmation to the user who made the selection
            messagingTemplate.convertAndSendToUser(
                    currentUser.getId().toString(),
                    "/queue/notifications",
                    WebSocketMessage.create("selection_confirmed", "Player selected successfully"));
            
        } catch (Exception e) {
            log.error("Error selecting player: {}", e.getMessage());
            
            // Try to send error message to the user if authenticated
            try {
                User currentUser = getCurrentUser(headerAccessor);
                if (currentUser != null) {
                    messagingTemplate.convertAndSendToUser(
                            currentUser.getId().toString(),
                            "/queue/errors",
                            WebSocketMessage.create("error", "Failed to select player: " + e.getMessage()));
                }
            } catch (Exception authException) {
                log.warn("Could not send error message to user: {}", authException.getMessage());
            }
        }
    }
    @MessageMapping("/match-events/{matchId}/generate-teams")
    @Operation(summary = "Generate teams", description = "Real-time team generation")
    public void generateTeams(@DestinationVariable UUID matchId, 
                            SimpMessageHeaderAccessor headerAccessor) {
        try {
            User currentUser = getCurrentUser(headerAccessor);
            log.info("User {} generating teams for match {}", currentUser.getId(), matchId);
            
            // Perform team generation
            matchService.generateTeams(matchId);
            
            // Get updated match data with all relationships loaded
            Match match = loadMatchWithRelationships(matchId);
            
            // Get available players (status = AVAILABLE) with users - eagerly fetched
            var availablePlayers = availabilityRepository.findAvailablePlayers(matchId);
            
            var matchResponse = MatchEventMapper.convertToResponse(match, availablePlayers);
            
            // Create and send update message
            MatchUpdateMessage message = MatchUpdateMessage.builder()
                    .action("TEAMS_GENERATED")
                    .matchId(matchId)
                    .matchTitle(match.getTitle())
                    .status(match.getStatus())
                    .message("Teams have been generated successfully")
                    .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                    .data(matchResponse)
                    .build();
            
            // Broadcast to all subscribers of this match
            messagingTemplate.convertAndSend("/topic/match/" + matchId + "/updates", 
                    WebSocketMessage.create("match_update", message));
            
        } catch (Exception e) {
            log.error("Error generating teams: {}", e.getMessage());
            
            // Send error message to the user
            User currentUser = getCurrentUser(headerAccessor);
            messagingTemplate.convertAndSendToUser(
                    currentUser.getId().toString(),
                    "/queue/errors",
                    WebSocketMessage.create("error", "Failed to generate teams: " + e.getMessage()));
        }
    }

    @MessageMapping("/match-events/{matchId}/start")
    @Operation(summary = "Start match", description = "Real-time match start")
    public void startMatch(@DestinationVariable UUID matchId, 
                         SimpMessageHeaderAccessor headerAccessor) {
        try {
            User currentUser = getCurrentUser(headerAccessor);
            log.info("User {} starting match {}", currentUser.getId(), matchId);
            
            // Start the match
            matchService.startMatch(matchId);
            
            // Get updated match data with all relationships loaded
            Match match = loadMatchWithRelationships(matchId);
            
            // Get available players (status = AVAILABLE) with users - eagerly fetched
            var availablePlayers = availabilityRepository.findAvailablePlayers(matchId);
            
            var matchResponse = MatchEventMapper.convertToResponse(match, availablePlayers);
            
            // Create and send update message
            MatchUpdateMessage message = MatchUpdateMessage.builder()
                    .action("MATCH_STARTED")
                    .matchId(matchId)
                    .matchTitle(match.getTitle())
                    .status(match.getStatus())
                    .message("Match has started!")
                    .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                    .data(matchResponse)
                    .build();
            
            // Broadcast to all subscribers of this match
            messagingTemplate.convertAndSend("/topic/match/" + matchId + "/updates", 
                    WebSocketMessage.create("match_update", message));
            
        } catch (Exception e) {
            log.error("Error starting match: {}", e.getMessage());
            
            // Send error message to the user
            User currentUser = getCurrentUser(headerAccessor);
            messagingTemplate.convertAndSendToUser(
                    currentUser.getId().toString(),
                    "/queue/errors",
                    WebSocketMessage.create("error", "Failed to start match: " + e.getMessage()));
        }
    }

    @MessageMapping("/match-events/{matchId}/complete")
    @Operation(summary = "Complete match", description = "Real-time match completion")
    public void completeMatch(@DestinationVariable UUID matchId, 
                            SimpMessageHeaderAccessor headerAccessor) {
        try {
            User currentUser = getCurrentUser(headerAccessor);
            log.info("User {} completing match {}", currentUser.getId(), matchId);
            
            // Complete the match
            matchService.completeMatch(matchId);
            
            // Get updated match data with all relationships loaded
            Match match = loadMatchWithRelationships(matchId);
            
            // Get available players (status = AVAILABLE) with users - eagerly fetched
            var availablePlayers = availabilityRepository.findAvailablePlayers(matchId);
            
            var matchResponse = MatchEventMapper.convertToResponse(match, availablePlayers);
            
            // Create and send update message
            MatchUpdateMessage message = MatchUpdateMessage.builder()
                    .action("MATCH_COMPLETED")
                    .matchId(matchId)
                    .matchTitle(match.getTitle())
                    .status(match.getStatus())
                    .message("Match has been completed!")
                    .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                    .data(matchResponse)
                    .build();
            
            // Broadcast to all subscribers of this match
            messagingTemplate.convertAndSend("/topic/match/" + matchId + "/updates", 
                    WebSocketMessage.create("match_update", message));
            
        } catch (Exception e) {
            log.error("Error completing match: {}", e.getMessage());
            
            // Send error message to the user
            User currentUser = getCurrentUser(headerAccessor);
            messagingTemplate.convertAndSendToUser(
                    currentUser.getId().toString(),
                    "/queue/errors",
                    WebSocketMessage.create("error", "Failed to complete match: " + e.getMessage()));
        }
    }

    @MessageMapping("/match-events/{matchId}/chat")
    @Operation(summary = "Send chat message", description = "Send a chat message to all participants in a match event")
    public void sendChatMessage(@DestinationVariable UUID matchId,
                               @Payload @Valid ChatMessageRequest request,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            User currentUser = getCurrentUser(headerAccessor);
            log.info("User {} sending chat message in match {}", currentUser.getId(), matchId);
            
            // Validate match ID matches
            if (!request.getMatchId().equals(matchId)) {
                throw new BadRequestException("Match ID mismatch");
            }
            
            // Verify match exists
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new BadRequestException("Match not found"));
            
            // Verify user is a participant in this match (has availability record)
            boolean isParticipant = availabilityRepository.existsByMatchIdAndUserId(matchId, currentUser.getId());
            if (!isParticipant) {
                throw new BadRequestException("You must be a participant in this match to send messages");
            }
            
            // Create and save chat message
            ChatMessage chatMessage = ChatMessage.builder()
                    .match(match)
                    .sender(currentUser)
                    .message(request.getMessage())
                    .build();
            chatMessage = chatMessageRepository.save(chatMessage);
            
            // Convert to response DTO
            ChatMessageResponse response = ChatMessageResponse.builder()
                    .id(chatMessage.getId())
                    .matchId(matchId)
                    .senderId(currentUser.getId())
                    .senderName(currentUser.getFirstName() + " " + currentUser.getLastName())
                    .senderFirstName(currentUser.getFirstName())
                    .senderLastName(currentUser.getLastName())
                    .message(chatMessage.getMessage())
                    .createdAt(chatMessage.getCreatedAt())
                    .build();
            
            // Broadcast to all subscribers of this match's chat
            String topic = "/topic/match/" + matchId + "/chat";
            WebSocketMessage wsMessage = WebSocketMessage.create("chat_message", response);
            messagingTemplate.convertAndSend(topic, wsMessage);
            
            log.info("Chat message broadcasted to topic: {}", topic);
            
            // Get all participants in the match (excluding sender)
            var availabilities = availabilityRepository.findByMatchIdWithUser(matchId);
            List<String> participantUserIds = availabilities.stream()
                    .map(availability -> availability.getUser().getId().toString())
                    .filter(userId -> !userId.equals(currentUser.getId().toString()))
                    .collect(java.util.stream.Collectors.toList());
            
            // Send push notifications to all participants except sender
            if (!participantUserIds.isEmpty()) {
                String senderName = currentUser.getFirstName() + " " + currentUser.getLastName();
                String notificationTitle = "New message in " + match.getTitle();
                String notificationBody = senderName + ": " + request.getMessage();
                
                Map<String, Object> notificationData = new java.util.HashMap<>();
                notificationData.put("type", "CHAT_MESSAGE");
                notificationData.put("matchId", matchId.toString());
                notificationData.put("messageId", chatMessage.getId().toString());
                notificationData.put("senderId", currentUser.getId().toString());
                notificationData.put("senderName", senderName);
                
                try {
                    pushNotificationService.sendPushNotificationToUsers(
                            participantUserIds,
                            notificationTitle,
                            notificationBody,
                            notificationData
                    );
                    log.info("Push notifications sent to {} participants", participantUserIds.size());
                } catch (Exception e) {
                    log.error("Error sending push notifications: {}", e.getMessage(), e);
                    // Don't fail the chat message if push notification fails
                }
            }
            
        } catch (Exception e) {
            log.error("Error sending chat message: {}", e.getMessage(), e);
            
            // Try to send error message to the user
            try {
                User currentUser = getCurrentUser(headerAccessor);
                if (currentUser != null) {
                    messagingTemplate.convertAndSendToUser(
                            currentUser.getId().toString(),
                            "/queue/errors",
                            WebSocketMessage.create("error", "Failed to send chat message: " + e.getMessage()));
                }
            } catch (Exception authException) {
                log.warn("Could not send error message to user: {}", authException.getMessage());
            }
        }
    }

    /**
     * Loads a Match entity with all necessary relationships eagerly fetched to avoid lazy loading issues.
     * This method loads:
     * - Community (eagerly)
     * - Teams with players and their users (eagerly)
     * - Teams' captains (eagerly)
     * - Availabilities with users (eagerly)
     * 
     * @param matchId The ID of the match to load
     * @return Match entity with all relationships loaded
     */
    private Match loadMatchWithRelationships(UUID matchId) {
        // Load match with Community eagerly to avoid lazy loading issues
        Match match = matchRepository.findByIdWithCommunity(matchId)
                .orElseThrow(() -> new BadRequestException("Match not found"));
        
        // Explicitly load teams with players and users using JOIN FETCH to avoid lazy loading issues
        // This fetches: teams, team players, player users, and team captains in a single query
        var teams = teamRepository.findByMatchIdWithPlayers(matchId);
        match.setTeams(teams);
        
        // Explicitly load availabilities with users (for total count calculation)
        // Use the method that eagerly fetches User to avoid lazy loading issues
        var availabilities = availabilityRepository.findByMatchIdWithUser(matchId);
        match.setAvailabilities(availabilities);
        
        log.debug("Loaded match {} with {} teams, {} availabilities", 
                matchId, teams.size(), availabilities.size());
        
        return match;
    }
    
    private User getCurrentUser(SimpMessageHeaderAccessor headerAccessor) {
        log.info("getCurrentUser called - Session ID: {}", headerAccessor != null ? headerAccessor.getSessionId() : "null");
        
        org.springframework.security.core.Authentication authentication = null;
        
        // CRITICAL: Always try headerAccessor first - this is set by the interceptor
        if (headerAccessor != null) {
            java.security.Principal principal = headerAccessor.getUser();
            log.info("HeaderAccessor.getUser() returned: {}", principal != null ? principal.getClass().getName() : "null");
            
            // The interceptor sets an Authentication object in headerAccessor.getUser()
            // UsernamePasswordAuthenticationToken implements both Principal and Authentication
            if (principal != null) {
                // Direct cast should work - interceptor sets Authentication here
                if (principal instanceof org.springframework.security.core.Authentication) {
                    authentication = (org.springframework.security.core.Authentication) principal;
                    log.info("✓ Direct cast successful - Authentication type: {}", authentication.getClass().getName());
                } else {
                    // Try explicit cast as fallback
                    try {
                        authentication = org.springframework.security.core.Authentication.class.cast(principal);
                        log.info("✓ Explicit cast successful - Authentication type: {}", authentication.getClass().getName());
                    } catch (ClassCastException e) {
                        log.error("✗ Cannot cast principal to Authentication. Principal type: {}, Error: {}", 
                                principal.getClass().getName(), e.getMessage());
                    }
                }
                
                // Verify authentication has a valid principal
                if (authentication != null) {
                    try {
                        Object authPrincipal = authentication.getPrincipal();
                        if (authPrincipal != null) {
                            String userName = authentication.getName();
                            log.info("✓✓ Authentication is valid (user: {}, principal type: {})", 
                                    userName, authPrincipal.getClass().getName());
                            
                            // CRITICAL: Set authentication in SecurityContext for this thread
                            // This ensures any code that accesses SecurityContext will find it
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.info("✓ Authentication set in SecurityContext from HeaderAccessor");
                        } else {
                            log.error("✗✗ Authentication exists but principal is NULL!");
                            authentication = null; // Reset if principal is null
                        }
                    } catch (Exception e) {
                        log.error("✗✗ Error accessing authentication principal: {}", e.getMessage(), e);
                        authentication = null; // Reset on error
                    }
                }
            }
            
            // Fallback: Check session attributes if authentication not found or invalid
            if (authentication == null || authentication.getPrincipal() == null) {
                Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
                log.info("Checking session attributes - Available keys: {}", 
                        sessionAttributes != null ? sessionAttributes.keySet() : "null");
                
                if (sessionAttributes != null) {
                    Object authObj = sessionAttributes.get("authentication");
                    if (authObj instanceof org.springframework.security.core.Authentication) {
                        authentication = (org.springframework.security.core.Authentication) authObj;
                        log.info("✓ Authentication retrieved from session attributes (user: {})", 
                                authentication.getName());
                        // Update headerAccessor for consistency
                        headerAccessor.setUser(authentication);
                        // CRITICAL: Also set in SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("✓ Authentication set in SecurityContext from session attributes");
                    }
                }
            }
        }
        
        // Last resort: SecurityContext (but this is often null in WebSocket context)
        if (authentication == null) {
            org.springframework.security.core.Authentication securityAuth = 
                    SecurityContextHolder.getContext().getAuthentication();
            if (securityAuth != null) {
                authentication = securityAuth;
                log.info("✓ Authentication found in SecurityContext (user: {})", authentication.getName());
            } else {
                log.warn("✗ SecurityContext.getAuthentication() is null");
            }
        }
        
        // Final validation
        if (authentication == null) {
            log.error("✗✗✗ No authentication found in any location! Session ID: {}", 
                    headerAccessor != null ? headerAccessor.getSessionId() : "null");
            throw new BadRequestException("Authentication required. Please provide a valid access token.");
        }
        
        // Verify principal exists
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            log.error("✗✗✗ Authentication found but principal is NULL! Authentication type: {}", 
                    authentication.getClass().getName());
            throw new BadRequestException("Invalid authentication: principal is null");
        }
        
        log.info("Authentication principal type: {}", principal.getClass().getName());
        
        // Extract user from principal
        if (principal instanceof AppUserDetail userDetail) {
            log.info("Principal is AppUserDetail, email: {}", userDetail.getUsername());
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }
        
        log.error("Invalid authentication principal type: {}. Expected AppUserDetail", principal.getClass().getName());
        throw new BadRequestException("Invalid authentication principal type: " + principal.getClass().getName());
    }
}
