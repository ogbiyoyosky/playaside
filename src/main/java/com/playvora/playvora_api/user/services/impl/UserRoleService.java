package com.playvora.playvora_api.user.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.community.repo.CommunityRepository;
import com.playvora.playvora_api.user.dtos.UserRoleRequest;
import com.playvora.playvora_api.user.dtos.UserRoleResponse;
import com.playvora.playvora_api.user.entities.Role;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.entities.UserRole;
import com.playvora.playvora_api.user.repo.RoleRepository;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.user.repo.UserRoleRepository;
import com.playvora.playvora_api.user.services.IUserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService implements IUserRoleService {
    
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CommunityRepository communityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getCurrentUserRoles() {
        User currentUser = getCurrentUser();
        List<UserRole> userRoles = userRoleRepository.findActiveUserRolesByUserId(currentUser.getId());
        return userRoles.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getCurrentUserRolesByCommunity(UUID communityId) {
        User currentUser = getCurrentUser();
        List<UserRole> userRoles = userRoleRepository.findActiveUserRolesByUserIdAndCommunityId(
                currentUser.getId(), communityId);
        return userRoles.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRoles(UUID userId) {
        // TODO: Add authorization check - only admins should call this
        List<UserRole> userRoles = userRoleRepository.findActiveUserRolesByUserId(userId);
        return userRoles.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRolesByCommunity(UUID userId, UUID communityId) {
        // TODO: Add authorization check - only admins or community managers should call this
        List<UserRole> userRoles = userRoleRepository.findActiveUserRolesByUserIdAndCommunityId(
                userId, communityId);
        return userRoles.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserRoleResponse assignRole(UserRoleRequest request) {
        // Verify user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        // Verify role exists
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new BadRequestException("Role not found"));
        
        // Verify community exists if provided
        Community community = null;
        if (request.getCommunityId() != null) {
            community = communityRepository.findById(request.getCommunityId())
                    .orElseThrow(() -> new BadRequestException("Community not found"));
        }
        
        // Check if role assignment already exists
        Optional<UserRole> existingUserRole = userRoleRepository.findActiveUserRole(
                request.getUserId(), 
                request.getRoleId(), 
                request.getCommunityId());
        
        if (existingUserRole.isPresent()) {
            throw new BadRequestException("User already has this role in the specified context");
        }
        
        // Create new user role
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .community(community)
                .build();
        
        UserRole savedUserRole = userRoleRepository.save(userRole);
        log.info("Assigned role {} to user {} in context {}", 
                role.getName(), user.getId(), 
                community != null ? "community " + community.getId() : "global");
        
        return toResponse(savedUserRole);
    }

    @Override
    @Transactional
    public void removeRole(UUID userRoleId) {
        UserRole userRole = userRoleRepository.findActiveUserRoleById(userRoleId)
                .orElseThrow(() -> new BadRequestException("User role not found or already inactive"));
        
        // TODO: Add authorization check - verify current user has permission to remove this role
        
        userRole.softDelete();
        userRoleRepository.save(userRole);
        
        log.info("Soft deleted user role: {}", userRoleId);
    }

    @Override
    public UserRoleResponse toResponse(UserRole userRole) {
        return UserRoleResponse.builder()
                .id(userRole.getId())
                .userId(userRole.getUser().getId())
                .roleId(userRole.getRole().getId())
                .roleName(userRole.getRole().getName())
                .roleDescription(userRole.getRole().getDescription())
                .communityId(userRole.getCommunity() != null ? userRole.getCommunity().getId() : null)
                .communityName(userRole.getCommunity() != null ? userRole.getCommunity().getName() : null)
                .createdAt(userRole.getCreatedAt())
                .isActive(userRole.isActive())
                .build();
    }
    
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }
        
        throw new BadRequestException("Invalid authentication principal");
    }
}

