package com.playvora.playvora_api.match.mappers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.playvora.playvora_api.match.dtos.TeamPlayerResponse;
import com.playvora.playvora_api.match.dtos.TeamResponse;
import com.playvora.playvora_api.match.entities.Team;
import com.playvora.playvora_api.match.entities.TeamPlayer;
import com.playvora.playvora_api.user.dtos.UserResponse;
import com.playvora.playvora_api.user.mappers.UserMapper;

public class TeamMapper {

    public static TeamResponse convertToResponse(Team team) {
        List<TeamPlayer> players = team.getPlayers();
        List<TeamPlayerResponse> playerResponses = players == null
                ? Collections.emptyList()
                : players.stream()
                        .map(TeamPlayerMapper::convertToResponse)
                        .collect(Collectors.toList());

        UserResponse captainResponse = team.getCaptain() == null
                ? null
                : UserMapper.convertToResponse(team.getCaptain());

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .color(team.getColor())
                .captain(captainResponse)
                .players(playerResponses)
                .build();
    }
}
