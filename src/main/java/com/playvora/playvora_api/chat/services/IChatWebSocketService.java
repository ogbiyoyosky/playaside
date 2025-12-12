package com.playvora.playvora_api.chat.services;

import com.playvora.playvora_api.chat.dtos.CommunityChatMessageRequest;
import com.playvora.playvora_api.chat.dtos.PrivateChatMessageRequest;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.UUID;

/**
 * WebSocket-facing chat service for private and community chats.
 * Implementation is responsible for auth, validation, persistence and broadcasting.
 */
public interface IChatWebSocketService {

    void sendPrivateChatMessage(UUID recipientId,
                                PrivateChatMessageRequest request,
                                SimpMessageHeaderAccessor headerAccessor);

    void sendCommunityChatMessage(UUID communityId,
                                  CommunityChatMessageRequest request,
                                  SimpMessageHeaderAccessor headerAccessor);
}


