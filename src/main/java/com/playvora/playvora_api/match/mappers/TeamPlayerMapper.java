package com.playvora.playvora_api.match.mappers;

import com.playvora.playvora_api.match.dtos.TeamPlayerResponse;
import com.playvora.playvora_api.match.entities.TeamPlayer;
import com.playvora.playvora_api.user.mappers.UserMapper;

public class TeamPlayerMapper {
    public static TeamPlayerResponse convertToResponse(TeamPlayer teamPlayer) {
        return TeamPlayerResponse.builder()
                .id(teamPlayer.getId())
                .user(UserMapper.convertToResponse(teamPlayer.getUser()))
                .isCaptain(teamPlayer.isCaptain())
                .createdAt(teamPlayer.getCreatedAt())
                .status(teamPlayer.getTeamAvailabilityStatus())
                .build();
    }
}
