package com.playvora.playvora_api.match.services;

import com.playvora.playvora_api.match.dtos.chat.ChatMessageRequest;
import com.playvora.playvora_api.match.dtos.websocket.PlayerSelectionRequest;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.UUID;

/**
 * Service interface for handling real-time match events over WebSocket.
 * Keeps WebSocket controllers thin by encapsulating business logic,
 * data loading, messaging and push notifications.
 */
public interface IMatchWebSocketService {

    void selectPlayer(UUID matchId,
                      PlayerSelectionRequest request,
                      SimpMessageHeaderAccessor headerAccessor);

    void generateTeams(UUID matchId,
                       SimpMessageHeaderAccessor headerAccessor);

    void startMatch(UUID matchId,
                    SimpMessageHeaderAccessor headerAccessor);

    void completeMatch(UUID matchId,
                       SimpMessageHeaderAccessor headerAccessor);

    void sendChatMessage(UUID matchId,
                         ChatMessageRequest request,
                         SimpMessageHeaderAccessor headerAccessor);
}


