package com.playvora.playvora_api.match.controllers;

import com.playvora.playvora_api.match.dtos.websocket.PlayerSelectionRequest;
import com.playvora.playvora_api.match.services.IMatchWebSocketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@Tag(name = "WebSocket", description = "Real-time WebSocket messaging for team selection")
public class WebSocketController {
    
    private final IMatchWebSocketService matchWebSocketService;

    @MessageMapping("/match-events/{matchId}/select-player")
    @Operation(summary = "Select player for team", description = "Real-time player selection for team")
    public void selectPlayer(@DestinationVariable UUID matchId, 
                           @Payload @Valid PlayerSelectionRequest request,
                           SimpMessageHeaderAccessor headerAccessor) {
        matchWebSocketService.selectPlayer(matchId, request, headerAccessor);
    }

    @MessageMapping("/match-events/{matchId}/generate-teams")
    @Operation(summary = "Generate teams", description = "Real-time team generation")
    public void generateTeams(@DestinationVariable UUID matchId, 
                            SimpMessageHeaderAccessor headerAccessor) {
        matchWebSocketService.generateTeams(matchId, headerAccessor);
    }

    @MessageMapping("/match-events/{matchId}/start")
    @Operation(summary = "Start match", description = "Real-time match start")
    public void startMatch(@DestinationVariable UUID matchId, 
                         SimpMessageHeaderAccessor headerAccessor) {
        matchWebSocketService.startMatch(matchId, headerAccessor);
    }

    @MessageMapping("/match-events/{matchId}/complete")
    @Operation(summary = "Complete match", description = "Real-time match completion")
    public void completeMatch(@DestinationVariable UUID matchId, 
                            SimpMessageHeaderAccessor headerAccessor) {
        matchWebSocketService.completeMatch(matchId, headerAccessor);
    }
}