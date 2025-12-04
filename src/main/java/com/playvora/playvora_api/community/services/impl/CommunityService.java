package com.playvora.playvora_api.community.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.exception.ForbiddenException;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.common.utils.UserRoleContext;
import com.playvora.playvora_api.community.dtos.AssignRoleRequest;
import com.playvora.playvora_api.community.dtos.CommunityResponse;
import com.playvora.playvora_api.community.dtos.CommunitySearchRequest;
import com.playvora.playvora_api.community.dtos.CreateCommunityRequest;
import com.playvora.playvora_api.community.dtos.UpdateCommunityRequest;
import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.community.entities.CommunityMember;
import com.playvora.playvora_api.community.mappers.CommunityMapper;
import com.playvora.playvora_api.community.repo.CommunityMemberRepository;
import com.playvora.playvora_api.community.repo.CommunityRepository;
import com.playvora.playvora_api.community.services.ICommunityService;
import com.playvora.playvora_api.community.services.IFileUploadService;
import com.playvora.playvora_api.match.enums.MatchStatus;
import com.playvora.playvora_api.match.repo.MatchRepository;
import com.playvora.playvora_api.user.entities.Role;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.entities.UserRole;
import com.playvora.playvora_api.user.repo.RoleRepository;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.user.repo.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityService implements ICommunityService {
    
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final IFileUploadService fileUploadService;
    private final MatchRepository matchRepository;


    @Override
    @Transactional
    public Community createCommunity(CreateCommunityRequest request) {
        if (communityRepository.existsByName(request.getName())) {
            throw new BadRequestException("Community with this name already exists");
        }
        
        User currentUser = getCurrentUser();

        Community community = Community.builder()
                .createdBy(currentUser)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .logoUrl(request.getLogoUrl())
                .bannerUrl(request.getBannerUrl())
                .province(request.getProvince())
                .country(request.getCountry())
                .postCode(request.getPostCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        
        Community savedCommunity = communityRepository.save(community);
        
        // Add creator as a member
        CommunityMember membership = CommunityMember.builder()
                .community(savedCommunity)
                .user(currentUser)
                .isActive(true)
                .build();
        communityMemberRepository.save(membership);
        
        // Add admin and community manager roles to creator for this community
        addRolesToUser(currentUser, savedCommunity, Set.of( "COMMUNITY_MANAGER"));
        
        return savedCommunity;
    }

    @Override
    @Transactional
    public Community updateCommunity(UUID id, UpdateCommunityRequest request) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Community not found"));
        
        // Validate that user has COMMUNITY_MANAGER role for this community
        validateCommunityManagerAccess(community.getId());
        
        // Safely handle Optional fields that might be null or empty
        if (request.getName() != null && request.getName().isPresent()) {
            String newName = request.getName().get();
            validateFieldLength(newName, "name", 255);
            if (!newName.equals(community.getName()) && communityRepository.existsByName(newName)) {
                throw new BadRequestException("Community with this name already exists");
            }
            community.setName(newName);
        }
        if (request.getDescription() != null && request.getDescription().isPresent()) {
            String description = request.getDescription().get();
            validateFieldLength(description, "description", 255);
            community.setDescription(description);
        }
        if (request.getLogoUrl() != null && request.getLogoUrl().isPresent()) {
            String logoUrl = request.getLogoUrl().get();
            validateFieldLength(logoUrl, "logoUrl", 255);
            community.setLogoUrl(logoUrl);
        }
        if (request.getBannerUrl() != null && request.getBannerUrl().isPresent()) {
            String bannerUrl = request.getBannerUrl().get();
            validateFieldLength(bannerUrl, "bannerUrl", 255);
            community.setBannerUrl(bannerUrl);
        }
        if (request.getAddress() != null && request.getAddress().isPresent()) {
            String address = request.getAddress().get();
            validateFieldLength(address, "address", 255);
            community.setAddress(address);
        }
        if (request.getCity() != null && request.getCity().isPresent()) {
            String city = request.getCity().get();
            validateFieldLength(city, "city", 255);
            community.setCity(city);
        }
        if (request.getProvince() != null && request.getProvince().isPresent()) {
            String province = request.getProvince().get();
            validateFieldLength(province, "province", 255);
            community.setProvince(province);
        }
        if (request.getCountry() != null && request.getCountry().isPresent()) {
            String country = request.getCountry().get();
            validateFieldLength(country, "country", 255);
            community.setCountry(country);
        }
        
        if (request.getLatitude() != null && request.getLatitude().isPresent()) {
            BigDecimal latitude = request.getLatitude().get();
            community.setLatitude(latitude);
        }
        if (request.getLongitude() != null && request.getLongitude().isPresent()) {
            BigDecimal longitude = request.getLongitude().get();
            community.setLongitude(longitude);
        }
        
        return communityRepository.save(community);
    }
    
    private void validateFieldLength(String value, String fieldName, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new BadRequestException(
                String.format("%s must not exceed %d characters (current length: %d)", 
                    fieldName, maxLength, value.length())
            );
        }
    }

    private String buildContainsFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return "%" + trimmed.toLowerCase() + "%";
    }

    @Override
    @Transactional
    public void deleteCommunity(UUID id) {
        Community community = communityRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Community not found"));
        
        // Validate that user has COMMUNITY_MANAGER role for this community
        validateCommunityManagerAccess(community.getId());

        // Prevent deletion if community has any upcoming or ongoing events
        boolean hasActiveEvents = matchRepository.hasActiveEventsForCommunity(
                community.getId(),
                java.util.List.of(
                        MatchStatus.UPCOMING,
                        MatchStatus.REGISTRATION_OPEN,
                        MatchStatus.REGISTRATION_CLOSED,
                        MatchStatus.TEAMS_SELECTED,
                        MatchStatus.TEAMS_MANUALLY_SELECTED,
                        MatchStatus.IN_PROGRESS
                )
        );

        if (hasActiveEvents) {
            throw new BadRequestException("Cannot delete community with ongoing or upcoming events");
        }

        // Remove the current user's roles for this community (e.g., COMMUNITY_MANAGER)
        User currentUser = getCurrentUser();
        java.util.List<UserRole> communityUserRoles =
                userRoleRepository.findActiveUserRolesByUserIdAndCommunityId(currentUser.getId(), community.getId());

        for (UserRole userRole : communityUserRoles) {
            userRole.softDelete(); // entities are managed; no explicit save needed
        }

        communityRepository.delete(community);
    }

    @Override
    public Community getCommunityById(UUID id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Community not found"));
    }

    @Override
    public List<String> getCommunitiesMetadata() {
        List<Community> communities = communityRepository.findAll();
        return communities.stream()
                .map(community -> community.getId().toString())
                .collect(Collectors.toList());
    }

    @Override
    public PaginatedResponse<CommunityResponse> searchCommunities(CommunitySearchRequest request) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(request.getSortDirection()) 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC, 
                request.getSortBy()
        );
        
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        
        Page<Community> communities = communityRepository.searchCommunities(
                buildContainsFilter(request.getName()),
                buildContainsFilter(request.getCity()),
                buildContainsFilter(request.getProvince()),
                buildContainsFilter(request.getCountry()),
                pageable
        );
        
        Page<CommunityResponse> responsePage = communities.map(this::convertToResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    public PaginatedResponse<CommunityResponse> getAllCommunities(int page, int size, String sortBy, String sortDirection, String search) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC, 
                sortBy
        );
        
        Pageable pageable = PageRequest.of(page, size, sort);
        String searchFilter = buildContainsFilter(search);
        Page<Community> communities;

        if (searchFilter == null) {
            communities = communityRepository.findAll(pageable);
        } else {
            communities = communityRepository.searchAllCommunities(searchFilter, pageable);
        }
        
        Page<CommunityResponse> responsePage = communities.map(this::convertToResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    @Transactional
    public void joinCommunity(UUID communityId) {
        User currentUser = getCurrentUser();
        Community community = getCommunityById(communityId);
        
        if (communityMemberRepository.existsByCommunityIdAndUserIdAndIsActiveTrue(communityId, currentUser.getId())) {
            throw new BadRequestException("User is already a member of this community");
        }
        
        CommunityMember membership = CommunityMember.builder()
                .community(community)
                .user(currentUser)
                .isActive(true)
                .build();
        
        communityMemberRepository.save(membership);
    }

    @Override
    @Transactional
    public void leaveCommunity(UUID communityId) {
        User currentUser = getCurrentUser();
        
        CommunityMember membership = communityMemberRepository
                .findByCommunityIdAndUserId(communityId, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("User is not a member of this community"));
        
        membership.setActive(false);
        communityMemberRepository.save(membership);
    }

    @Override
    public boolean isUserMember(UUID communityId) {
        User currentUser = getCurrentUser();
        return communityMemberRepository.existsByCommunityIdAndUserIdAndIsActiveTrue(communityId, currentUser.getId());
    }

    @Override
    public PaginatedResponse<CommunityResponse> getUserCommunities(int page, int size, String search) {
        User currentUser = getCurrentUser();
        
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        List<CommunityMember> memberships = communityMemberRepository
                .findActiveMembershipsByUserId(currentUser.getId());
        
        List<UUID> communityIds = memberships.stream()
                .map(membership -> membership.getCommunity().getId())
                .collect(Collectors.toList());
        
        if (communityIds.isEmpty()) {
            Page<CommunityResponse> emptyPage = Page.empty(pageable);
            return PaginationUtils.toPaginatedResponse(emptyPage);
        }
        
        List<Community> communityList = communityRepository.findAllById(communityIds);

        // Optional in-memory search filter by name/city/province/country
        String searchFilter = buildContainsFilter(search);
        if (searchFilter != null) {
            String normalized = searchFilter.replace("%", "");
            communityList = communityList.stream()
                    .filter(c ->
                            (c.getName() != null && c.getName().toLowerCase().contains(normalized)) ||
                            (c.getCity() != null && c.getCity().toLowerCase().contains(normalized)) ||
                            (c.getProvince() != null && c.getProvince().toLowerCase().contains(normalized)) ||
                            (c.getCountry() != null && c.getCountry().toLowerCase().contains(normalized))
                    )
                    .collect(Collectors.toList());
        }
        
        // Manual pagination since findAllById doesn't support Pageable
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), communityList.size());
        List<Community> pagedCommunities = communityList.subList(start, end);
        
        Page<Community> communities = new PageImpl<>(
                pagedCommunities, pageable, communityList.size());
        Page<CommunityResponse> responsePage = communities.map(this::convertToResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    @Transactional
    public String uploadCommunityLogo(UUID communityId, MultipartFile file) {
        Community community = getCommunityById(communityId);
        
        // Validate that user has COMMUNITY_MANAGER role for this community
        validateCommunityManagerAccess(communityId);
        
        // Delete old logo if exists
        if (community.getLogoUrl() != null) {
            fileUploadService.deleteImage(community.getLogoUrl());
        }
        
        // Upload new logo
        String logoUrl = fileUploadService.uploadImage(file, "community-logos");
        community.setLogoUrl(logoUrl);
        communityRepository.save(community);
        
        return logoUrl;
    }

    @Override
    @Transactional
    public String uploadCommunityBanner(UUID communityId, MultipartFile file) {
        Community community = getCommunityById(communityId);
        
        // Validate that user has COMMUNITY_MANAGER role for this community
        validateCommunityManagerAccess(communityId);
        
        // Delete old banner if exists
        if (community.getBannerUrl() != null) {
            fileUploadService.deleteImage(community.getBannerUrl());
        }
        
        // Upload new banner
        String bannerUrl = fileUploadService.uploadImage(file, "community-banners");
        community.setBannerUrl(bannerUrl);
        communityRepository.save(community);
        
        return bannerUrl;
    }

    @Override
    @Transactional
    public void assignRoleToMember(UUID communityId, AssignRoleRequest request) {
        // Verify community exists
        Community community = getCommunityById(communityId);
        
        // Validate that user has COMMUNITY_MANAGER role for this community
        validateCommunityManagerAccess(communityId);
        
        // Verify user exists and is a member
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        if (!communityMemberRepository.existsByCommunityIdAndUserIdAndIsActiveTrue(communityId, request.getUserId())) {
            throw new BadRequestException("User is not a member of this community");
        }
        
        // Add role to user for this community
        addRolesToUser(user, community, Set.of(request.getRole()));
    }

    private void addRolesToUser(User user, Community community, Set<String> roleNames) {
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BadRequestException("Role not found: " + roleName));
            
            // Check if user already has this role in this community
            java.util.Optional<UserRole> existingRole = userRoleRepository.findActiveUserRole(
                    user.getId(), 
                    role.getId(), 
                    community != null ? community.getId() : null);
            
            if (existingRole.isEmpty()) {
                // Create new user role assignment
                UserRole userRole = UserRole.builder()
                        .user(user)
                        .role(role)
                        .community(community)
                        .build();
                userRoleRepository.save(userRole);
                
                log.info("Assigned role {} to user {} in community {}", 
                        roleName, user.getId(), 
                        community != null ? community.getId() : "global");
            } else {
                log.debug("User {} already has role {} in community {}", 
                        user.getId(), roleName, 
                        community != null ? community.getId() : "global");
            }
        }
    }

    /**
     * Validate that the current user has COMMUNITY_MANAGER role for the specified community.
     * Also allows ROLE_ADMIN (global admin) to access any community.
     * 
     * @param communityId The community ID to validate access for
     * @throws ForbiddenException if user doesn't have required permissions
     */
    private void validateCommunityManagerAccess(UUID communityId) {
        User currentUser = getCurrentUser();
        
        // Check if user has ROLE_ADMIN (global admin can access any community)
        boolean isAdmin = userRoleRepository.hasRole(
            currentUser.getId(), 
            "ROLE_ADMIN", 
            null  // Global role
        );
        
        if (isAdmin) {
            log.debug("User {} has ROLE_ADMIN, granting access to community {}", currentUser.getId(), communityId);
            return;
        }
        
        // Check UserRoleContext first (if X-User-Role-Id header was provided)
        UserRole currentUserRole = UserRoleContext.getCurrentUserRole();
        if (currentUserRole != null && currentUserRole.isActive()) {
            // Verify the role in context is COMMUNITY_MANAGER and matches the community
            if ("COMMUNITY_MANAGER".equals(currentUserRole.getRole().getName())) {
                if (currentUserRole.getCommunity() != null && 
                    currentUserRole.getCommunity().getId().equals(communityId)) {
                    log.debug("User {} has COMMUNITY_MANAGER role for community {} in context", 
                        currentUser.getId(), communityId);
                    return;
                } else {
                    throw new ForbiddenException(
                        "User does not have COMMUNITY_MANAGER role for community " + communityId);
                }
            }
        }
        
        // Fallback: Check database directly if context is not set
        boolean hasManagerRole = userRoleRepository.hasRole(
            currentUser.getId(), 
            "COMMUNITY_MANAGER", 
            communityId
        );
        
        if (!hasManagerRole) {
            throw new ForbiddenException(
                "User does not have COMMUNITY_MANAGER role for community " + communityId);
        }
        
        log.debug("User {} has COMMUNITY_MANAGER role for community {} (verified from database)", 
            currentUser.getId(), communityId);
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }
        
        throw new BadRequestException("Invalid authentication principal");
    }

    private CommunityResponse convertToResponse(Community community) {
        User currentUser = null;
        try {
            currentUser = getCurrentUser();
        } catch (Exception e) {
            // User not authenticated, isMember will be false
        }
        
        Long memberCount = communityMemberRepository.countActiveMembersByCommunityId(community.getId());
        boolean isMember = currentUser != null &&
                communityMemberRepository.existsByCommunityIdAndUserIdAndIsActiveTrue(community.getId(), currentUser.getId());

        return CommunityMapper.convertToResponse(community, memberCount, isMember);
    }

    @Override
    public Long getCommunityMemberCount(UUID communityId) {
        return communityMemberRepository.countActiveMembersByCommunityId(communityId);
    }

    @Override
    public boolean isUserMemberOfCommunity(UUID userId, UUID communityId) {
        return communityMemberRepository.existsByCommunityIdAndUserIdAndIsActiveTrue(communityId, userId);
    }
}
