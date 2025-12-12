package com.playvora.playvora_api.common.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse<T> {
    @Builder.Default
    private boolean success = true;
    private String message;
    private T data;
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public static <T> SuccessResponse<T> of(T data, String message) {
        return SuccessResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> SuccessResponse<T> of(String message) {
        return SuccessResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}

