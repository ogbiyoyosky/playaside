package com.playvora.playvora_api.match.dtos.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WebSocketMessage {
    private String type;
    private String messageId;
    private OffsetDateTime timestamp;
    private Object data;
    private String message;
    
    public static WebSocketMessage create(String type, Object data) {
        return WebSocketMessage.builder()
                .type(type)
                .messageId(java.util.UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .data(data)
                .build();
    }
    
    public static WebSocketMessage create(String type, Object data, String message) {
        return WebSocketMessage.builder()
                .type(type)
                .messageId(java.util.UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .data(data)
                .message(message)
                .build();
    }
}
