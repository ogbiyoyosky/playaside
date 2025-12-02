package com.playvora.playvora_api.match.dtos.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamSelectionMessage {
    private String action; // SELECT_PLAYER, REMOVE_PLAYER, ASSIGN_CAPTAIN, GENERATE_TEAMS
    private UUID matchId;
    private UUID teamId;
    private String teamName;
    private UUID userId;
    private String userName;
    private String message;
    private Object data; // Full match data after the change
}
