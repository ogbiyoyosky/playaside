package com.playvora.playvora_api.match.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import com.playvora.playvora_api.match.enums.TeamAvailabilityStatus;
import com.playvora.playvora_api.user.dtos.UserResponse;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamPlayerResponse {
    private UUID id;
    private UserResponse user;
    private boolean isCaptain;
    private LocalDateTime createdAt;
    private TeamAvailabilityStatus status;
}
