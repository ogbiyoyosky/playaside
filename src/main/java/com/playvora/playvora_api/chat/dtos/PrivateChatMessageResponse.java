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
public class PrivateChatMessageResponse {

    private UUID id;
    private UUID senderId;
    private String senderName;
    private UUID recipientId;
    private String recipientName;
    private String message;
    private OffsetDateTime createdAt;
    private String senderProfilePictureUrl;
    private String recipientProfilePictureUrl;
    /**
     * A stable identifier for the conversation between two users.
     * This is useful on the client to group messages into a single thread.
     */
    private String conversationId;
}


