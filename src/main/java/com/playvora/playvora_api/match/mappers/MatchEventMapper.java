package com.playvora.playvora_api.match.mappers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.playvora.playvora_api.community.mappers.CommunityMapper;
import com.playvora.playvora_api.match.dtos.MatchEventResponse;
import com.playvora.playvora_api.match.dtos.TeamResponse;
import com.playvora.playvora_api.match.entities.Availability;
import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.match.entities.Team;
import com.playvora.playvora_api.match.enums.AvailabilityStatus;

public class MatchEventMapper {

    private MatchEventMapper() {
        // Utility class
    }

    public static MatchEventResponse convertToResponse(Match match) {
        List<TeamResponse> teams = match.getTeams() == null
                ? Collections.emptyList()
                : match.getTeams().stream()
                        .map(TeamMapper::convertToResponse)
                        .collect(Collectors.toList());

        List<UUID> draftOrder = parseDraftOrder(match.getManualDraftOrder());
        UUID currentPickingTeamId = match.getCurrentPickingTeamId();

        String currentPickerName = null;
        UUID currentPickerId = null;
        String currentPickingTeamName = null;

        // IMPORTANT: Avoid touching Match.currentPickingTeam directly because it is a LAZY proxy
        // and WebSocket flows may run without an open Hibernate session. Instead, resolve the
        // current picking team from the already-loaded teams collection.
        if (currentPickingTeamId != null && match.getTeams() != null) {
            Team currentPickingTeam = match.getTeams().stream()
                    .filter(t -> currentPickingTeamId.equals(t.getId()))
                    .findFirst()
                    .orElse(null);

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
        }

        MatchEventResponse response = MatchEventResponse.builder()
                .id(match.getId())
                .community(CommunityMapper.convertToResponse(match.getCommunity()))
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

        Integer availabilitiesCount = availabilities != null ? availabilities.stream()
                .filter(a -> a.getStatus() == AvailabilityStatus.AVAILABLE)
                .collect(Collectors.toList()).size() : 0;
        
        // Use provided available players list or filter from match availabilities
        response.setAvailablePlayers(availabilitiesCount);
        response.setTotalPlayers(totalPlayers);
        response.setPlayersAvailability(availabilities != null ? availabilities.stream()
                .map(PlayerAvialabilityMapper::convertToResponse)
                .collect(Collectors.toList()) : Collections.emptyList());
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
