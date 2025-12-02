package com.playvora.playvora_api.files.dtos;

import org.springframework.core.io.Resource;

public record FileResourcePayload(Resource resource, String contentType, String originalFileName) {
}

