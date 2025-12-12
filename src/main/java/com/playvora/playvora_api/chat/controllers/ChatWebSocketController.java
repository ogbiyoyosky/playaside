package com.playvora.playvora_api.chat.controllers;

import com.playvora.playvora_api.chat.dtos.CommunityChatMessageRequest;
import com.playvora.playvora_api.chat.dtos.PrivateChatMessageRequest;
import com.playvora.playvora_api.chat.services.IChatWebSocketService;
import com.playvora.playvora_api.match.dtos.chat.ChatMessageRequest;
import com.playvora.playvora_api.match.services.IMatchWebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket Chat", description = "Real-time WebSocket messaging for private, community, and match event chats")
public class ChatWebSocketController {

    private final IChatWebSocketService chatWebSocketService;
    private final IMatchWebSocketService matchWebSocketService;

    @MessageMapping("/chat/private/{recipientId}")
    @Operation(summary = "Send private chat message",
               description = "Send a private chat message between two users")
    public void sendPrivateChatMessage(@DestinationVariable UUID recipientId,
                                       @Payload @Valid PrivateChatMessageRequest request,
                                       SimpMessageHeaderAccessor headerAccessor) {
        chatWebSocketService.sendPrivateChatMessage(recipientId, request, headerAccessor);
    }

    @MessageMapping("/communities/{communityId}/chat")
    @Operation(summary = "Send community chat message",
               description = "Send a chat message to all members of a community")
    public void sendCommunityChatMessage(@DestinationVariable UUID communityId,
                                         @Payload @Valid CommunityChatMessageRequest request,
                                         SimpMessageHeaderAccessor headerAccessor) {
        chatWebSocketService.sendCommunityChatMessage(communityId, request, headerAccessor);
    }

    @MessageMapping("/match-events/{matchId}/chat")
    @Operation(summary = "Send match event chat message",
               description = "Send a chat message to all participants in a match event")
    public void sendMatchEventChatMessage(@DestinationVariable UUID matchId,
                                          @Payload @Valid ChatMessageRequest request,
                                          SimpMessageHeaderAccessor headerAccessor) {
        matchWebSocketService.sendChatMessage(matchId, request, headerAccessor);
    }
}


