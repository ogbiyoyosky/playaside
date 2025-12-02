package com.playvora.playvora_api.user.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import com.playvora.playvora_api.community.dtos.CommunityResponse;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponse {
    private UUID id;
    private UUID userId;
    private UUID roleId;
    private String roleName;
    private String roleDescription;
    private UUID communityId;
    private CommunityResponse community;
    private String communityName;
    private LocalDateTime createdAt;
    private boolean isActive;
}

