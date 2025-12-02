package com.playvora.playvora_api.user.services;

import com.playvora.playvora_api.user.dtos.UserRoleRequest;
import com.playvora.playvora_api.user.dtos.UserRoleResponse;
import com.playvora.playvora_api.user.entities.UserRole;

import java.util.List;
import java.util.UUID;

public interface IUserRoleService {
    
    /**
     * Get all active roles for the current authenticated user
     */
    List<UserRoleResponse> getCurrentUserRoles();
    
    /**
     * Get all active roles for the current user in a specific community
     */
    List<UserRoleResponse> getCurrentUserRolesByCommunity(UUID communityId);
    
    /**
     * Get all active roles for a specific user (admin function)
     */
    List<UserRoleResponse> getUserRoles(UUID userId);
    
    /**
     * Get all active roles for a specific user in a specific community (admin function)
     */
    List<UserRoleResponse> getUserRolesByCommunity(UUID userId, UUID communityId);
    
    /**
     * Assign a role to a user (optionally tied to a community)
     */
    UserRoleResponse assignRole(UserRoleRequest request);
    
    /**
     * Remove a role from a user (soft delete)
     */
    void removeRole(UUID userRoleId);
    
    /**
     * Convert UserRole entity to response DTO
     */
    UserRoleResponse toResponse(UserRole userRole);
}

