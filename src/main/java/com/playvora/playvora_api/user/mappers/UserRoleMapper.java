package com.playvora.playvora_api.user.mappers;

import com.playvora.playvora_api.user.dtos.UserRoleResponse;
import com.playvora.playvora_api.user.entities.UserRole;

import java.util.UUID;

public class UserRoleMapper {

    public static UserRoleResponse convertToResponse(UserRole userRole) {

        UUID communityId = userRole.getCommunity() != null ? userRole.getCommunity().getId() : null;
        String communityName = userRole.getCommunity() != null ? userRole.getCommunity().getName() : null;
        return UserRoleResponse.builder()
                .id(userRole.getId())
                .userId(userRole.getUser().getId())
                .roleId(userRole.getRole().getId())
                .roleName(userRole.getRole().getName())
                .roleDescription(userRole.getRole().getDescription())
                .communityId(communityId)
                .communityName(communityName)
                .build();
    }
}
