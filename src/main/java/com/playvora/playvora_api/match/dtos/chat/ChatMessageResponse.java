package com.playvora.playvora_api.match.dtos.chat;

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
public class ChatMessageResponse {
    private UUID id;
    private UUID matchId;
    private UUID senderId;
    private String senderName;
    private String senderFirstName;
    private String senderLastName;
    private String message;
    private String senderProfilePictureUrl;
    private OffsetDateTime createdAt;
}


