package com.playvora.playvora_api.user.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.community.entities.CommunityMember;
import com.playvora.playvora_api.match.entities.MatchRegistration;
import com.playvora.playvora_api.match.repo.MatchRegistrationRepository;
import com.playvora.playvora_api.user.dtos.RegisterRequest;
import com.playvora.playvora_api.user.dtos.RegisterUserResponse;
import com.playvora.playvora_api.user.dtos.UpdateRequest;
import com.playvora.playvora_api.user.dtos.UserLimited;
import com.playvora.playvora_api.user.dtos.UserResponse;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.mappers.UserMapper;
import com.playvora.playvora_api.user.services.IUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping(path = "/api/v1/users")
@Tag(name = "User", description = "User management APIs")
@RequiredArgsConstructor
public class UserController {
    private final IUserService userService;
    private final MatchRegistrationRepository matchRegistrationRepository;

    @PostMapping(path = "/register")
    @Operation(summary = "Register user", description = "Register a new user")
    public ResponseEntity<ApiResponse<RegisterUserResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {

        User user = userService.createUser(registerRequest);
        RegisterUserResponse registerUserResponse = RegisterUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .nickname(user.getNickname())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .country(user.getCountry())
                .profilePictureUrl(user.getProfilePictureUrl())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(registerUserResponse, "User registered successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @PatchMapping(path = "/update")
    @Operation(summary = "Update user", description = "Update the current user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@Valid @RequestBody UpdateRequest updateRequest) {
        User user = userService.updateCurrentUser(updateRequest);
        UserResponse userResponse = UserMapper.convertToResponse(user);

        // Populate registered events map for the current user
        populateRegisteredEvents(user, userResponse);
        populateRegisteredCommunities(user, userResponse);

        return ResponseEntity.ok(ApiResponse.success(userResponse, "User updated successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping(path = "/me")
    @Operation(summary = "Get current user", description = "Get the current user")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        User user = userService.getCurrentUser();
        UserResponse userResponse = UserMapper.convertToResponse(user);

        // Populate registered events map for the current user
        populateRegisteredEvents(user, userResponse);
        populateRegisteredCommunities(user, userResponse);
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User fetched successfully"));


    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping(path = "/{userId}")
    @Operation(summary = "Get user by ID", description = "Get a user by their ID")
    public ResponseEntity<ApiResponse<UserLimited>> getUserById(@PathVariable UUID userId) {
        User user = userService.getUserById(userId);
        UserLimited userResponse = UserMapper.convertToLimited(user);
        return ResponseEntity.ok(ApiResponse.success(userResponse, "User fetched successfully"));
    }

    /**
     * Populate the registeredEvents map on the UserResponse with all matches
     * the user has explicitly joined (via MatchRegistration).
     *
     * The map key is the match/event ID and the value is always true for joined matches.
     */
    private void populateRegisteredEvents(User user, UserResponse userResponse) {
        List<MatchRegistration> registrations = matchRegistrationRepository.findByUserId(user.getId());
        Map<UUID, Boolean> registeredEvents = new HashMap<>();

        for (MatchRegistration registration : registrations) {
            if (registration.getMatch() != null && registration.getMatch().getId() != null) {
                registeredEvents.put(registration.getMatch().getId(), Boolean.TRUE);
            }
        }

        userResponse.setRegisteredEvents(registeredEvents);
    }

    private void populateRegisteredCommunities(User user, UserResponse userResponse) {
        List<CommunityMember> memberships = userService.getUserCommunities(user.getId());
        Map<UUID, Boolean> registeredCommunities = new HashMap<>();

        for (CommunityMember membership : memberships) {
            registeredCommunities.put(membership.getCommunity().getId(), Boolean.TRUE);
        }
        userResponse.setRegisteredCommunities(registeredCommunities);
    }

   
}
    
