package com.playvora.playvora_api.files.dtos;

import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserFileResponse {
    String fileName;
    String originalFileName;
    String contentType;
    long size;
    String url;
    OffsetDateTime uploadedAt;
}

