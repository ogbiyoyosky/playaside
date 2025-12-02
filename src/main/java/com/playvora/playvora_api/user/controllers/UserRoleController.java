package com.playvora.playvora_api.user.controllers;

import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.user.dtos.UserRoleRequest;
import com.playvora.playvora_api.user.dtos.UserRoleResponse;
import com.playvora.playvora_api.user.services.IUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/user-roles")
@RequiredArgsConstructor
@Tag(name = "User Roles", description = "User role management endpoints")
@SecurityRequirement(name = "BearerAuth")
@Slf4j
public class UserRoleController {
    
    private final IUserRoleService userRoleService;

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/me")
    @Operation(
        summary = "Get current user's roles",
        description = "Returns all active roles for the authenticated user across all communities"
    )
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getCurrentUserRoles() {
        List<UserRoleResponse> userRoles = userRoleService.getCurrentUserRoles();
        return ResponseEntity.ok(ApiResponse.success(userRoles, "User roles retrieved successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/me/community/{communityId}")
    @Operation(
        summary = "Get current user's roles in a community",
        description = "Returns all active roles for the authenticated user in a specific community"
    )
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getCurrentUserRolesByCommunity(
            @PathVariable UUID communityId) {
        List<UserRoleResponse> userRoles = userRoleService.getCurrentUserRolesByCommunity(communityId);
        return ResponseEntity.ok(ApiResponse.success(userRoles, "User roles retrieved successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get user's roles (Admin)",
        description = "Returns all active roles for a specific user. Admin only."
    )
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getUserRoles(
            @PathVariable UUID userId) {
        List<UserRoleResponse> userRoles = userRoleService.getUserRoles(userId);
        return ResponseEntity.ok(ApiResponse.success(userRoles, "User roles retrieved successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/user/{userId}/community/{communityId}")
    @Operation(
        summary = "Get user's roles in a community (Admin/Community Manager)",
        description = "Returns all active roles for a specific user in a community. Admin or Community Manager only."
    )
    public ResponseEntity<ApiResponse<List<UserRoleResponse>>> getUserRolesByCommunity(
            @PathVariable UUID userId,
            @PathVariable UUID communityId) {
        List<UserRoleResponse> userRoles = userRoleService.getUserRolesByCommunity(userId, communityId);
        return ResponseEntity.ok(ApiResponse.success(userRoles, "User roles retrieved successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    @Operation(
        summary = "Assign role to user",
        description = "Assigns a role to a user, optionally tied to a community. Admin only."
    )
    public ResponseEntity<ApiResponse<UserRoleResponse>> assignRole(
            @Valid @RequestBody UserRoleRequest request) {
        UserRoleResponse userRole = userRoleService.assignRole(request);
        return ResponseEntity.ok(ApiResponse.success(userRole, "Role assigned successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{userRoleId}")
    @Operation(
        summary = "Remove role from user",
        description = "Soft deletes a user role assignment. Admin only."
    )
    public ResponseEntity<ApiResponse<Void>> removeRole(
            @PathVariable UUID userRoleId) {
        userRoleService.removeRole(userRoleId);
        return ResponseEntity.ok(ApiResponse.success("Role removed successfully"));
    }
}

