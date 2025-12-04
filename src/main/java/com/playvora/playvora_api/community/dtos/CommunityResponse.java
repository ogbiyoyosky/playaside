package com.playvora.playvora_api.community.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommunityResponse {
    private UUID id;
    private String name;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String address;
    private String city;
    private String province;
    private String country;
    private String postCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long memberCount;
    private boolean isMember;
}
