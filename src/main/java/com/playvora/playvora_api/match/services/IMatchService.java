package com.playvora.playvora_api.match.services;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.match.dtos.AvailabilityRequest;
import com.playvora.playvora_api.match.dtos.CreateMatchRequest;
import com.playvora.playvora_api.match.dtos.MatchEventResponse;
import com.playvora.playvora_api.match.dtos.JoinMatchRequest;
import com.playvora.playvora_api.match.dtos.UpdateMatchRequest;
import com.playvora.playvora_api.match.entities.Match;

import java.util.List;
import java.util.UUID;

public interface IMatchService {
    Match createMatchEvent(CreateMatchRequest request);
    Match updateMatchEvent(UUID id, UpdateMatchRequest request);
    void deleteMatchEvent(UUID id);
    Match getMatchEventById(UUID id);
    PaginatedResponse<MatchEventResponse> getMatchEvents(int page, int size, String sortBy, String sortDirection, String search);
    PaginatedResponse<MatchEventResponse> getMatchEventsByCommunity(UUID communityId, int page, int size, String sortBy, String sortDirection);
    PaginatedResponse<MatchEventResponse> getUpcomingMatchEvents(int page, int size, String search);
    PaginatedResponse<MatchEventResponse> getUserMatchEvents(int page, int size, String search);
    void markAvailability(UUID matchId, AvailabilityRequest request);
    void removeAvailability(UUID matchId);
    Match generateTeams(UUID matchId);
    void selectPlayerForTeam(UUID matchId, UUID teamId, UUID userId);
    void removePlayerFromTeam(UUID matchId, UUID teamId, UUID userId);
    void assignCaptain(UUID teamId, UUID userId);
    void cancelMatch(UUID matchId);
    void startMatch(UUID matchId);
    void addAvailability(UUID matchId, AvailabilityRequest request);
    void completeMatch(UUID matchId);
    void joinMatchEvent(UUID matchId, JoinMatchRequest request);
    List<String> getMatchEventsMetadata();
}
