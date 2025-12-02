package com.playvora.playvora_api.match.mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.playvora.playvora_api.match.dtos.MatchEventResponse;
import com.playvora.playvora_api.match.dtos.TeamResponse;
import com.playvora.playvora_api.match.entities.Availability;
import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.match.entities.Team;
import com.playvora.playvora_api.match.enums.AvailabilityStatus;
import com.playvora.playvora_api.user.dtos.UserResponse;
import com.playvora.playvora_api.user.mappers.UserMapper;

public class MatchEventMapper {

    private MatchEventMapper() {
        // Utility class
    }

    public static MatchEventResponse convertToResponse(Match match) {
        return convertToResponse(match, null);
    }

    public static MatchEventResponse convertToResponse(Match match, List<Availability> availablePlayersList) {
        List<TeamResponse> teams = match.getTeams() == null
                ? Collections.emptyList()
                : match.getTeams().stream()
                        .map(TeamMapper::convertToResponse)
                        .collect(Collectors.toList());

        List<UUID> draftOrder = parseDraftOrder(match.getManualDraftOrder());
        Team currentPickingTeam = match.getCurrentPickingTeam();

        String currentPickerName = null;
        UUID currentPickerId = null;
        String currentPickingTeamName = null;

        if (currentPickingTeam != null) {
            currentPickingTeamName = currentPickingTeam.getName();
            if (currentPickingTeam.getCaptain() != null) {
                currentPickerId = currentPickingTeam.getCaptain().getId();
                String firstName = currentPickingTeam.getCaptain().getFirstName();
                String lastName = currentPickingTeam.getCaptain().getLastName();
                StringBuilder nameBuilder = new StringBuilder();
                if (firstName != null && !firstName.isBlank()) {
                    nameBuilder.append(firstName.trim());
                }
                if (lastName != null && !lastName.isBlank()) {
                    if (nameBuilder.length() > 0) {
                        nameBuilder.append(" ");
                    }
                    nameBuilder.append(lastName.trim());
                }
                currentPickerName = nameBuilder.length() > 0 ? nameBuilder.toString() : null;
            }
        }

        MatchEventResponse response = MatchEventResponse.builder()
                .id(match.getId())
                .communityId(match.getCommunity() != null ? match.getCommunity().getId() : null)
                .communityName(match.getCommunity() != null ? match.getCommunity().getName() : null)
                .title(match.getTitle())
                .description(match.getDescription())
                .type(match.getType())
                .matchDate(match.getMatchDate())
                .registrationDeadline(match.getRegistrationDeadline())
                .playersPerTeam(match.getPlayersPerTeam())
                .status(match.getStatus())
                .isAutoSelection(match.isAutoSelection())
                .createdAt(match.getCreatedAt())
                .updatedAt(match.getUpdatedAt())
                .teams(teams)
                .draftInProgress(match.getDraftInProgress())
                .currentPickingTeamId(match.getCurrentPickingTeamId())
                .currentPickingTeamName(currentPickingTeamName)
                .currentPickerId(currentPickerId)
                .currentPickerName(currentPickerName)
                .draftOrder(draftOrder)
                .isPaidEvent(match.getIsPaidEvent())
                .isRefundable(match.getIsRefundable())
                .maxPlayers(match.getMaxPlayers())
                .currency(match.getCurrency())
                .pricePerPlayer(match.getPricePerPlayer())  
                .address(match.getAddress())
                .city(match.getCity())
                .province(match.getProvince())
                .postCode(match.getPostCode())
                .country(match.getCountry())
                .latitude(match.getLatitude())
                .longitude(match.getLongitude())
                .gender(match.getGender())
                .bannerUrl(match.getBannerUrl())
                .draftIndex(match.getManualDraftIndex())
                .build();

        List<Availability> availabilities = match.getAvailabilities();
        int totalPlayers = availabilities == null ? 0 : availabilities.size();
        
        // Use provided available players list or filter from match availabilities
        List<Availability> availableAvailabilities = availablePlayersList != null 
                ? availablePlayersList 
                : (availabilities == null ? Collections.emptyList() :
                    availabilities.stream()
                        .filter(availability -> availability.getStatus() == AvailabilityStatus.AVAILABLE)
                        .collect(Collectors.toList()));
        
        int availablePlayersCount = availableAvailabilities.size();
        
        // Map available players to UserResponse
        List<UserResponse> availablePlayersResponse = availableAvailabilities.stream()
                .map(availability -> UserMapper.convertToResponse(availability.getUser()))
                .collect(Collectors.toList());

        response.setTotalPlayers(totalPlayers);
        response.setAvailablePlayers(availablePlayersCount);
        response.setAvailablePlayersList(availablePlayersResponse);

        return response;
    }

    private static List<UUID> parseDraftOrder(String manualDraftOrder) {
        if (manualDraftOrder == null || manualDraftOrder.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(manualDraftOrder.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

}
