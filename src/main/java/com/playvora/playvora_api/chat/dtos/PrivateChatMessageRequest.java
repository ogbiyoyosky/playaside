package com.playvora.playvora_api.chat.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateChatMessageRequest {

    private UUID recipientId;

    @NotBlank(message = "Message cannot be empty")
    private String message;
}


