package com.playvora.playvora_api.chat.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatNotification {

    public enum ChatType {
        PRIVATE,
        COMMUNITY,
        MATCH_EVENT
    }

    private ChatType chatType;
    private UUID targetId;  // recipientId, communityId, or matchId
    private UUID messageId;
    private UUID senderId;
    private String senderName;
    private String preview; // Optional: first ~50 chars of message
    private OffsetDateTime timestamp;
}