package com.playvora.playvora_api.match.controllers;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.match.dtos.AvailabilityRequest;
import com.playvora.playvora_api.match.dtos.CreateMatchRequest;
import com.playvora.playvora_api.match.dtos.MatchEventResponse;
import com.playvora.playvora_api.match.dtos.JoinMatchRequest;
import com.playvora.playvora_api.match.dtos.UpdateMatchRequest;
import com.playvora.playvora_api.match.dtos.websocket.MatchUpdateMessage;
import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.match.dtos.websocket.TeamSelectionMessage;
import com.playvora.playvora_api.match.repo.AvailabilityRepository;
import com.playvora.playvora_api.match.repo.MatchRepository;
import com.playvora.playvora_api.match.repo.TeamRepository;
import com.playvora.playvora_api.match.services.IMatchService;
import com.playvora.playvora_api.match.mappers.MatchEventMapper;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/match-events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Match Events", description = "REST endpoints for real-time match events (WebSocket alternative)")
public class MatchEventController {
    
    private final IMatchService matchService;
    private final UserRepository userRepository;
    private final AvailabilityRepository availabilityRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;


    @PostMapping
    @Operation(summary = "Create match Event", description = "Create a new match")
    public ResponseEntity<ApiResponse<MatchEventResponse>> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        Match match = matchService.createMatchEvent(request);
        MatchEventResponse response  = MatchEventMapper.convertToResponse(match);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Match created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all match events", description = "Get all match events with pagination and optional search")
    public ResponseEntity<ApiResponse<PaginatedResponse<MatchEventResponse>>> getAllMatchEvents(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "matchDate") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "Search term (matches title, description, or community name)")
            @RequestParam(required = false) String search
    ) {
        PaginatedResponse<MatchEventResponse> matches = matchService.getMatchEvents(page, size, sortBy, sortDirection, search);
        return ResponseEntity.ok(ApiResponse.success(matches, "Match events retrieved successfully"));
    }

    @GetMapping("/metadata")
    @Operation(summary = "Get all match events", description = "Get all match events with pagination and optional search")
    public ResponseEntity<ApiResponse<List<String>>> getAllMatchEventsMetadata() {
        List<String> matches = matchService.getMatchEventsMetadata();
        return ResponseEntity.ok(ApiResponse.success(matches, "Match events retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get match by ID", description = "Get a match by its ID")
    public ResponseEntity<ApiResponse<MatchEventResponse>> getMatchById(
            @Parameter(description = "Match ID") @PathVariable UUID id) {
        // Load match with Community eagerly to avoid lazy loading issues
        Match match = matchRepository.findByIdWithCommunity(id)
                .orElseThrow(() -> new BadRequestException("Match not found"));
        
        // Explicitly load teams with players and users using JOIN FETCH to avoid lazy loading issues
        // This fetches teams, their players, and player users in a single query
        var teams = teamRepository.findByMatchIdWithPlayers(id);
        match.setTeams(teams);
        
        // Explicitly load availabilities with users (for total count calculation)
        // Use the method that eagerly fetches User to avoid lazy loading issues
        var availabilities = availabilityRepository.findByMatchIdWithUser(id);
        match.setAvailabilities(availabilities);
        
        MatchEventResponse response = MatchEventMapper.convertToResponse(match);
        return ResponseEntity.ok(ApiResponse.success(response, "Match retrieved successfully"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update match", description = "Update an existing match")
    public ResponseEntity<ApiResponse<MatchEventResponse>> updateMatch(
            @Parameter(description = "Match ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateMatchRequest request) {
        Match match = matchService.updateMatchEvent(id, request);
        MatchEventResponse response = MatchEventMapper.convertToResponse(match);
        return ResponseEntity.ok(ApiResponse.success(response, "Match updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete match", description = "Delete a match")
    public ResponseEntity<ApiResponse<Void>> deleteMatch(
            @Parameter(description = "Match ID") @PathVariable UUID id) {
        matchService.deleteMatchEvent(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Match deleted successfully"));
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Get match by ID", description = "Get a match by its ID")
    public ResponseEntity<ApiResponse<MatchEventResponse>> getMatchByIdMetadata(
            @Parameter(description = "Match ID") @PathVariable UUID id) {
        Match match = matchService.getMatchEventById(id);
        MatchEventResponse response = MatchEventMapper.convertToResponse(match);
        return ResponseEntity.ok(ApiResponse.success(response, "Match retrieved successfully"));
    }

    @GetMapping("/community/{communityId}")
    @Operation(summary = "Get matches by community", description = "Get all matches for a specific community")
    public ResponseEntity<ApiResponse<PaginatedResponse<MatchEventResponse>>> getMatchesByCommunity(
            @Parameter(description = "Community ID") @PathVariable UUID communityId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "matchDate") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDirection) {
        
            PaginatedResponse<MatchEventResponse> matches = matchService.getMatchEventsByCommunity(communityId, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(matches, "Community matches retrieved successfully"));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming matches", description = "Get all upcoming matches")
    public ResponseEntity<ApiResponse<PaginatedResponse<MatchEventResponse>>> getUpcomingMatches(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search term (matches title, description, or community name)")
            @RequestParam(required = false) String search) {
        
        PaginatedResponse<MatchEventResponse> matches = matchService.getUpcomingMatchEvents(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(matches, "Upcoming matches retrieved successfully"));
    }

    @GetMapping("/my-matches")
    @Operation(summary = "Get user matches", description = "Get matches that the current user has marked availability for")
    public ResponseEntity<ApiResponse<PaginatedResponse<MatchEventResponse>>> getUserMatches(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search term (matches title, description, or community name)")
            @RequestParam(required = false) String search) {
        
        PaginatedResponse<MatchEventResponse> matches = matchService.getUserMatchEvents(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(matches, "User matches retrieved successfully"));
    }


    @DeleteMapping("/{id}/availability")
    @Operation(summary = "Remove availability", description = "Remove availability for a match")
    public ResponseEntity<ApiResponse<Void>> removeAvailability(
            @Parameter(description = "Match ID") @PathVariable UUID id) {
        matchService.removeAvailability(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Availability removed successfully"));
    }


    @PostMapping("/{matchId}/teams/{teamId}/select-player/{userId}")
    @Operation(summary = "Select player for team", description = "Select a player for a specific team")
    public ResponseEntity<ApiResponse<MatchEventResponse>> selectPlayerForTeam(
            @Parameter(description = "Match ID") @PathVariable UUID matchId,
            @Parameter(description = "Team ID") @PathVariable UUID teamId,
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        matchService.selectPlayerForTeam(matchId, teamId, userId);
        // Load match with Community eagerly to avoid lazy loading issues
        Match match = matchRepository.findByIdWithCommunity(matchId)
                .orElseThrow(() -> new BadRequestException("Match not found"));
        MatchEventResponse response = MatchEventMapper.convertToResponse(match);
        return ResponseEntity.ok(ApiResponse.success(response, "Player selected successfully"));
    }

    @DeleteMapping("/{matchId}/teams/{teamId}/remove-player/{userId}")
    @Operation(summary = "Remove player from team", description = "Remove a player from a specific team")
    public ResponseEntity<ApiResponse<Void>> removePlayerFromTeam(
            @Parameter(description = "Match ID") @PathVariable UUID matchId,
            @Parameter(description = "Team ID") @PathVariable UUID teamId,
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        matchService.removePlayerFromTeam(matchId, teamId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Player removed successfully"));
    }

    @PostMapping("/{matchId}/mark-availability")
    @Operation(summary = "Add availability", description = "Add availability for a match event")
    public ResponseEntity<ApiResponse<Void>> markAvailability(
            @RequestBody AvailabilityRequest request,
            @Parameter(description = "Match ID") @PathVariable UUID matchId) {
        matchService.markAvailability(
                matchId,request
        );
        return ResponseEntity.ok(ApiResponse.success(null, "Availability added successfully"));
    }

    @PostMapping("/teams/{teamId}/assign-captain/{userId}")
    @Operation(summary = "Assign captain", description = "Assign a captain to a team")
    public ResponseEntity<ApiResponse<Void>> assignCaptain(
            @Parameter(description = "Team ID") @PathVariable UUID teamId,
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        matchService.assignCaptain(teamId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Captain assigned successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel match", description = "Cancel a match")
    public ResponseEntity<ApiResponse<Void>> cancelMatch(
            @Parameter(description = "Match ID") @PathVariable UUID id) {
        matchService.cancelMatch(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Match cancelled successfully"));
    }


    @PostMapping("/match/{matchId}/select-player")
    @Operation(summary = "Select player for team", description = "Select a player for a team")
    public ResponseEntity<ApiResponse<TeamSelectionMessage>> selectPlayer(
            @Parameter(description = "Match ID") @PathVariable UUID matchId,
            @Parameter(description = "Team ID") @RequestParam UUID teamId,
            @Parameter(description = "User ID") @RequestParam UUID userId
            ) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("User {} selecting player {} for team {} in match {}", 
                    currentUser.getId(), userId, teamId, matchId);
            
            // Perform the selection
            matchService.selectPlayerForTeam(matchId, teamId, userId);
            
            // Get updated match data with available players
            var match = matchService.getMatchEventById(matchId);
            var matchResponse = MatchEventMapper.convertToResponse(match);
            var selectedUser = userRepository.findById(userId)
                    .orElseThrow(() -> new BadRequestException("User not found"));
            
            // Create response message
            TeamSelectionMessage message = TeamSelectionMessage.builder()
                    .action("SELECT_PLAYER")
                    .matchId(matchId)
                    .teamId(teamId)
                    .userId(userId)
                    .userName(selectedUser.getFirstName() + " " + selectedUser.getLastName())
                    .teamName("Team " + teamId)
                    .message(selectedUser.getFirstName() + " " + selectedUser.getLastName() + " has been selected for the team")
                    .data(matchResponse)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(message, "Player selected successfully"));
            
        } catch (Exception e) {
            log.error("Error selecting player: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to select player: " + e.getMessage()));
        }
    }

    @PostMapping("/match/{matchId}/remove-player")
    @Operation(summary = "Remove player from team", description = "Remove a player from a team")
    public ResponseEntity<ApiResponse<TeamSelectionMessage>> removePlayer(
            @Parameter(description = "Match ID") @PathVariable UUID matchId,
            @Parameter(description = "Team ID") @RequestParam UUID teamId,
            @Parameter(description = "User ID") @RequestParam UUID userId) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("User {} removing player {} from team {} in match {}", 
                    currentUser.getId(), userId, teamId, matchId);
            
            // Perform the removal
            matchService.removePlayerFromTeam(matchId, teamId, userId);
            
            // Get updated match data with available players
            var match = matchService.getMatchEventById(matchId);
            var matchResponse = MatchEventMapper.convertToResponse(match);
            var removedUser = userRepository.findById(userId)
                    .orElseThrow(() -> new BadRequestException("User not found"));
            
            // Create response message
            TeamSelectionMessage message = TeamSelectionMessage.builder()
                    .action("REMOVE_PLAYER")
                    .matchId(matchId)
                    .teamId(teamId)
                    .userId(userId)
                    .userName(removedUser.getFirstName() + " " + removedUser.getLastName())
                    .teamName("Team " + teamId)
                    .message(removedUser.getFirstName() + " " + removedUser.getLastName() + " has been removed from the team")
                    .data(matchResponse)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(message, "Player removed successfully"));
            
        } catch (Exception e) {
            log.error("Error removing player: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to remove player: " + e.getMessage()));
        }
    }

    @PostMapping("/match/{matchId}/assign-captain")
    @Operation(summary = "Assign captain to team", description = "Assign a captain to a team")
    public ResponseEntity<ApiResponse<TeamSelectionMessage>> assignCaptain(
            @Parameter(description = "Match ID") @PathVariable UUID matchId,
            @Parameter(description = "Team ID") @RequestParam UUID teamId,
            @Parameter(description = "User ID") @RequestParam UUID userId) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("User {} assigning captain {} to team {} in match {}", 
                    currentUser.getId(), userId, teamId, matchId);
            
            // Perform the captain assignment
            matchService.assignCaptain(teamId, userId);
            
            // Get updated match data with available players
            var match = matchService.getMatchEventById(matchId);
            var matchResponse = MatchEventMapper.convertToResponse(match);
            var captainUser = userRepository.findById(userId)
                    .orElseThrow(() -> new BadRequestException("User not found"));
            
            // Create response message
            TeamSelectionMessage message = TeamSelectionMessage.builder()
                    .action("ASSIGN_CAPTAIN")
                    .matchId(matchId)
                    .teamId(teamId)
                    .userId(userId)
                    .userName(captainUser.getFirstName() + " " + captainUser.getLastName())
                    .teamName("Team " + teamId)
                    .message(captainUser.getFirstName() + " " + captainUser.getLastName() + " has been assigned as captain")
                    .data(matchResponse)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(message, "Captain assigned successfully"));
            
        } catch (Exception e) {
            log.error("Error assigning captain: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to assign captain: " + e.getMessage()));
        }
    }

    @PostMapping("/{matchId}/join")
    @Operation(summary = "Join match event", description = "Join a match event. For paid events, the correct payment amount must be provided and will be recorded.")
    public ResponseEntity<ApiResponse<Void>> joinMatch(
            @Parameter(description = "Match ID") @PathVariable UUID matchId,
            @Valid @RequestBody(required = false) JoinMatchRequest request) {
        matchService.joinMatchEvent(matchId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Joined match successfully"));
    }

    @PostMapping("/match/{matchId}/generate-teams")
    @Operation(summary = "Generate teams", description = "Generate teams for a match")
    public ResponseEntity<ApiResponse<MatchEventResponse>> generateTeams(
            @Parameter(description = "Match ID") @PathVariable UUID matchId) {
            Match match = matchService.generateTeams(matchId);
            
                // Get available players and create response
            MatchEventResponse message = MatchEventMapper.convertToResponse(match);
            return ResponseEntity.ok(ApiResponse.success(message, "Teams generated successfully"));
        
    }

    @PostMapping("/match/{matchId}/start")
    @Operation(summary = "Start match", description = "Start a match")
    public ResponseEntity<ApiResponse<MatchUpdateMessage>> startMatch(
            @Parameter(description = "Match ID") @PathVariable UUID matchId) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("User {} starting match {}", currentUser.getId(), matchId);
            
            // Start the match
            matchService.startMatch(matchId);
            
            // Get updated match data
            var match = matchService.getMatchEventById(matchId);
            
            // Create response message
            MatchUpdateMessage message = MatchUpdateMessage.builder()
                    .action("MATCH_STARTED")
                    .matchId(matchId)
                    .matchTitle(match.getTitle())
                    .status(match.getStatus())
                    .message("Match has started!")
                    .timestamp(OffsetDateTime.now())
                    .data(match)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(message, "Match started successfully"));
            
        } catch (Exception e) {
            log.error("Error starting match: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to start match: " + e.getMessage()));
        }
    }

    @PostMapping("/match/{matchId}/complete")
    @Operation(summary = "Complete match", description = "Complete a match")
    public ResponseEntity<ApiResponse<MatchUpdateMessage>> completeMatch(
            @Parameter(description = "Match ID") @PathVariable UUID matchId) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("User {} completing match {}", currentUser.getId(), matchId);
            
            // Complete the match
            matchService.completeMatch(matchId);
            
            // Get updated match data
            var match = matchService.getMatchEventById(matchId);
            
            // Create response message
            MatchUpdateMessage message = MatchUpdateMessage.builder()
                    .action("MATCH_COMPLETED")
                    .matchId(matchId)
                    .matchTitle(match.getTitle())
                    .status(match.getStatus())
                    .message("Match has been completed!")
                    .timestamp(OffsetDateTime.now())
                    .data(match)
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(message, "Match completed successfully"));
            
        } catch (Exception e) {
            log.error("Error completing match: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to complete match: " + e.getMessage()));
        }
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }
        
        throw new BadRequestException("Invalid authentication principal");
    }
}
