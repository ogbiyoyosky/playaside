package com.playvora.playvora_api.community.services;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.community.dtos.AssignRoleRequest;
import com.playvora.playvora_api.community.dtos.CommunityResponse;
import com.playvora.playvora_api.community.dtos.CommunitySearchRequest;
import com.playvora.playvora_api.community.dtos.CreateCommunityRequest;
import com.playvora.playvora_api.community.dtos.UpdateCommunityRequest;
import com.playvora.playvora_api.community.entities.Community;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ICommunityService {
    Community createCommunity(CreateCommunityRequest request);
    Community updateCommunity(UUID id, UpdateCommunityRequest request);
    void deleteCommunity(UUID id);
    Community getCommunityById(UUID id);
    PaginatedResponse<CommunityResponse> searchCommunities(CommunitySearchRequest request);
    PaginatedResponse<CommunityResponse> getAllCommunities(int page, int size, String sortBy, String sortDirection, String search);
    void joinCommunity(UUID communityId);
    void leaveCommunity(UUID communityId);
    boolean isUserMember(UUID communityId);
    PaginatedResponse<CommunityResponse> getUserCommunities(int page, int size, String search);
    String uploadCommunityLogo(UUID communityId, MultipartFile file);
    String uploadCommunityBanner(UUID communityId, MultipartFile file);
    void assignRoleToMember(UUID communityId, AssignRoleRequest request);
    List<String> getCommunitiesMetadata();
    Long getCommunityMemberCount(UUID id);
    boolean isUserMemberOfCommunity(UUID userId, UUID communityId);
}
