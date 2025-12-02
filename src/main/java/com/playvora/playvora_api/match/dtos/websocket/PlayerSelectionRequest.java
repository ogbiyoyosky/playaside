package com.playvora.playvora_api.match.dtos.websocket;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerSelectionRequest {
    @NotNull(message = "Match ID is required")
    private UUID matchId;
    
    @NotNull(message = "Team ID is required")
    private UUID teamId;
    
    @NotNull(message = "User ID is required")
    private UUID userId;
}
