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
public class CommunityChatMessageResponse {

    private UUID id;
    private UUID communityId;
    private UUID senderId;
    private String senderName;
    private String message;
    private String senderProfilePictureUrl;
    private OffsetDateTime createdAt;
}


