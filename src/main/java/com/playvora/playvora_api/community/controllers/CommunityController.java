package com.playvora.playvora_api.community.controllers;

import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.community.dtos.AssignRoleRequest;
import com.playvora.playvora_api.community.dtos.CommunityResponse;
import com.playvora.playvora_api.community.dtos.CreateCommunityRequest;
import com.playvora.playvora_api.community.dtos.UpdateCommunityRequest;
import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.community.services.ICommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/communities")
@Tag(name = "Community", description = "Community management APIs")
@RequiredArgsConstructor
public class CommunityController {
    
    private final ICommunityService communityService;

    @PostMapping
    @Operation(summary = "Create community", description = "Create a new community")
    public ResponseEntity<ApiResponse<Community>> createCommunity(@Valid @RequestBody CreateCommunityRequest request) {
        Community community = communityService.createCommunity(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(community, "Community created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get community by ID", description = "Get a community by its ID")
    public ResponseEntity<ApiResponse<CommunityResponse>> getCommunityById(
            @Parameter(description = "Community ID") @PathVariable UUID id) {
        Community community = communityService.getCommunityById(id);
        CommunityResponse response = convertToResponse(community);
        return ResponseEntity.ok(ApiResponse.success(response, "Community retrieved successfully"));
    }


    @GetMapping("/{id}/metadata")
    @Operation(summary = "Get community by ID", description = "Get a community by its ID")
    public ResponseEntity<ApiResponse<CommunityResponse>> getCommunityByIdMetadata(
            @Parameter(description = "Community ID") @PathVariable UUID id) {
        Community community = communityService.getCommunityById(id);
        CommunityResponse response = convertToResponse(community);
        return ResponseEntity.ok(ApiResponse.success(response, "Community retrieved successfully"));
    }



    @PatchMapping("/{id}")
    @Operation(summary = "Update community", description = "Update an existing community")
    public ResponseEntity<ApiResponse<Community>> updateCommunity(
            @Parameter(description = "Community ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateCommunityRequest request) {
        Community community = communityService.updateCommunity(id, request);
        return ResponseEntity.ok(ApiResponse.success(community, "Community updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete community", description = "Delete a community")
    public ResponseEntity<ApiResponse<Void>> deleteCommunity(
            @Parameter(description = "Community ID") @PathVariable UUID id) {
        communityService.deleteCommunity(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Community deleted successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all communities", description = "Get all communities with pagination")
    public ResponseEntity<ApiResponse<PaginatedResponse<CommunityResponse>>> getAllCommunities(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDirection,
            @Parameter(description = "Search term (matches name, city, province, or country)")
            @RequestParam(required = false) String search) {
        
        PaginatedResponse<CommunityResponse> communities = communityService.getAllCommunities(page, size, sortBy, sortDirection, search);
        return ResponseEntity.ok(ApiResponse.success(communities, "Communities retrieved successfully"));
    }

    @GetMapping("/metadata")
    @Operation(summary = "Get all communities", description = "Get all communities with pagination")
    public ResponseEntity<ApiResponse<List<String>>> getAllCommunitiesMetadata() {
        
        List<String> communities = communityService.getCommunitiesMetadata();
        return ResponseEntity.ok(ApiResponse.success(communities, "Communities retrieved successfully"));
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join community", description = "Join a community")
    public ResponseEntity<ApiResponse<Void>> joinCommunity(
            @Parameter(description = "Community ID") @PathVariable UUID id) {
        communityService.joinCommunity(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Successfully joined community"));
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Leave community", description = "Leave a community")
    public ResponseEntity<ApiResponse<Void>> leaveCommunity(
            @Parameter(description = "Community ID") @PathVariable UUID id) {
        communityService.leaveCommunity(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Successfully left community"));
    }

    @GetMapping("/{id}/membership")
    @Operation(summary = "Check membership", description = "Check if current user is a member of the community")
    public ResponseEntity<ApiResponse<Boolean>> checkMembership(
            @Parameter(description = "Community ID") @PathVariable UUID id) {
        boolean isMember = communityService.isUserMember(id);
        return ResponseEntity.ok(ApiResponse.success(isMember, "Membership status retrieved"));
    }

    @GetMapping("/my-communities")
    @Operation(summary = "Get user communities", description = "Get communities that the current user is a member of")
    public ResponseEntity<ApiResponse<PaginatedResponse<CommunityResponse>>> getUserCommunities(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search term (matches name, city, province, or country)")
            @RequestParam(required = false) String search) {
        
        PaginatedResponse<CommunityResponse> communities = communityService.getUserCommunities(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(communities, "User communities retrieved successfully"));
    }


    @PostMapping("/{id}/assign-role")
    @Operation(summary = "Assign role to member", description = "Assign a role to a community member")
    public ResponseEntity<ApiResponse<Void>> assignRoleToMember(
            @Parameter(description = "Community ID") @PathVariable UUID id,
            @Valid @RequestBody AssignRoleRequest request) {
        communityService.assignRoleToMember(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Role assigned successfully"));
    }

    private CommunityResponse convertToResponse(Community community) {
        // This is a simplified version - in a real implementation, you might want to
        // inject the service or use a mapper to get the full response with member count
        return CommunityResponse.builder()
                .id(community.getId())
                .name(community.getName())
                .description(community.getDescription())
                .logoUrl(community.getLogoUrl())
                .bannerUrl(community.getBannerUrl())
                .address(community.getAddress())
                .city(community.getCity())
                .province(community.getProvince())
                .country(community.getCountry())
                .postCode(community.getPostCode())
                .latitude(community.getLatitude())
                .longitude(community.getLongitude())
                .createdAt(community.getCreatedAt())
                .updatedAt(community.getUpdatedAt())
                .build();
    }
}
