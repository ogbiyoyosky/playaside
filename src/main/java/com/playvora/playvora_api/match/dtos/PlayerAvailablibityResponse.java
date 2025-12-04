package com.playvora.playvora_api.match.dtos;

import java.util.UUID;

import com.playvora.playvora_api.match.enums.AvailabilityStatus;
import com.playvora.playvora_api.user.dtos.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PlayerAvailablibityResponse {
    private UUID userId;
    private UserResponse user;
    private AvailabilityStatus status;
}
