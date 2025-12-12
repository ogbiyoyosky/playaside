package com.playvora.playvora_api.chat.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityChatMessageRequest {

    @NotNull(message = "Community ID is required")
    private UUID communityId;

    @NotBlank(message = "Message cannot be empty")
    private String message;
}


