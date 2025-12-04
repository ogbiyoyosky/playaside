package com.playvora.playvora_api.match.mappers;

import com.playvora.playvora_api.match.entities.Availability;
import com.playvora.playvora_api.match.dtos.PlayerAvailablibityResponse;
import com.playvora.playvora_api.user.dtos.UserResponse;

public class PlayerAvialabilityMapper {
    public static PlayerAvailablibityResponse convertToResponse(Availability availability) {
        return PlayerAvailablibityResponse.builder()
                .userId(availability.getUser().getId())
                .user(UserResponse.builder()
                        .id(availability.getUser().getId())
                        .email(availability.getUser().getEmail())
                        .firstName(availability.getUser().getFirstName())
                        .lastName(availability.getUser().getLastName())
                        .nickname(availability.getUser().getNickname())
                        .profilePictureUrl(availability.getUser().getProfilePictureUrl())
                        .build())
                .status(availability.getStatus())
                .build();
    }
}
