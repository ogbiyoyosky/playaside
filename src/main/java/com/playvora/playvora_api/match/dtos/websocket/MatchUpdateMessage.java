package com.playvora.playvora_api.match.dtos.websocket;

import com.playvora.playvora_api.match.enums.MatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MatchUpdateMessage {
    private String action; // STATUS_CHANGED, MATCH_STARTED, MATCH_COMPLETED, MATCH_CANCELED
    private UUID matchId;
    private String matchTitle;
    private MatchStatus status;
    private String message;
    private LocalDateTime timestamp;
    private Object data; // Full match data after the change
}
