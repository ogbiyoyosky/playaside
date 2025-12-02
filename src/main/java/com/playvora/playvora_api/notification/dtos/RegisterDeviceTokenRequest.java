         package com.playvora.playvora_api.notification.dtos;

import com.playvora.playvora_api.notification.enums.DeviceType;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {
    @NotBlank(message = "Token is required")
    private String token;
    
    private DeviceType deviceType; // e.g., "ios", "android"
    
    private String deviceId; // Optional: unique device identifier
}

