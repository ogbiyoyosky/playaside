package com.playvora.playvora_api.community.mappers;

import com.playvora.playvora_api.community.dtos.CommunityResponse;
import com.playvora.playvora_api.community.entities.Community;

public class CommunityMapper {

    private CommunityMapper() {
        // Utility class
    }

    public static CommunityResponse convertToResponse(Community community) {
        if (community == null) {
            return null;
        }

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
                .createdById(community.getCreatedBy() != null ? community.getCreatedBy().getId() : null)
                .createdAt(community.getCreatedAt())
                .updatedAt(community.getUpdatedAt())
                .build();
    }

    public static CommunityResponse convertToResponse(Community community, Long memberCount, boolean isMember) {
        if (community == null) {
            return null;
        }

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
                .createdById(community.getCreatedBy() != null ? community.getCreatedBy().getId() : null)
                .createdAt(community.getCreatedAt())
                .updatedAt(community.getUpdatedAt())
                .memberCount(memberCount)
                .isMember(isMember)
                .build();
    }
}
