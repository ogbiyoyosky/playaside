package com.playvora.playvora_api.match.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.exception.ForbiddenException;
import com.playvora.playvora_api.common.utils.CurrencyMapper;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.common.utils.UserRoleContext;
import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.community.entities.CommunityMember;
import com.playvora.playvora_api.community.repo.CommunityRepository;
import com.playvora.playvora_api.match.dtos.*;
import com.playvora.playvora_api.match.dtos.JoinMatchRequest;
import com.playvora.playvora_api.match.entities.*;
import com.playvora.playvora_api.match.enums.AvailabilityStatus;
import com.playvora.playvora_api.match.enums.MatchStatus;
import com.playvora.playvora_api.match.enums.TeamAvailabilityStatus;
import com.playvora.playvora_api.match.repo.*;
import com.playvora.playvora_api.match.services.IMatchService;
import com.playvora.playvora_api.notification.services.IPushNotificationService;
import com.playvora.playvora_api.payment.entities.Payment;
import com.playvora.playvora_api.payment.enums.PaymentStatus;
import com.playvora.playvora_api.payment.enums.PaymentType;
import com.playvora.playvora_api.payment.repo.PaymentRepository;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.entities.UserRole;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.user.repo.UserRoleRepository;
import com.playvora.playvora_api.community.repo.CommunityMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.playvora.playvora_api.match.mappers.MatchEventMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService implements IMatchService {
    
    private final MatchRepository matchRepository;
    private final AvailabilityRepository availabilityRepository;
    private final TeamRepository teamRepository;
    private final TeamPlayerRepository teamPlayerRepository;
    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final IPushNotificationService pushNotificationService;
    private final CommunityMemberRepository communityMemberRepository;
    private final PaymentRepository paymentRepository;
    private final MatchRegistrationRepository matchRegistrationRepository;

    @Override
    @Transactional
    public Match createMatchEvent(CreateMatchRequest request) {
        // Validate community exists
        Community community = communityRepository.findById(request.getCommunityId())
                .orElseThrow(() -> new BadRequestException("Community not found"));
        
        // Validate that user has COMMUNITY_MANAGER role for this community
        validateCommunityManagerAccess(community.getId());
        
        // Validate dates
        if (request.getMatchDate().isBefore(request.getRegistrationDeadline())) {
            throw new BadRequestException("Match date must be after registration deadline");
        }
        
        if (request.getRegistrationDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Registration deadline must be in the future");
        }

        //get the country of the current user
        User currentUser = getCurrentUser();
        String country = currentUser.getCountry();
        String currency = CurrencyMapper.getCurrency(country);
        
        Match match = Match.builder()
                .createdBy(currentUser)
                .community(community)
                .title(request.getTitle())
                .description(request.getDescription())
                .bannerUrl(request.getBannerUrl())
                .matchDate(request.getMatchDate())
                .registrationDeadline(request.getRegistrationDeadline())
                .currency(currency)
                .type(request.getType().name())
                .pricePerPlayer(request.getPricePerPlayer())
                .isPaidEvent(request.getIsPaidEvent())
                .isRefundable(request.getIsRefundable())
                .maxPlayers(request.getMaxPlayers())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .city(request.getCity())
                .province(request.getProvince())
                .postCode(request.getPostCode())
                .country(request.getCountry())
                .gender(request.getGender().name())
                .playersPerTeam(request.getPlayersPerTeam())
                .isAutoSelection(request.getIsAutoSelection())
                .status(MatchStatus.UPCOMING)
                .build();
        
        Match savedMatch = matchRepository.save(match);
        
        // Send push notification to community members about new match
        try {
            List<UUID> memberUserIds = communityMemberRepository.findActiveMembersByCommunityId(community.getId())
                    .stream()
                    .map(member -> member.getUser().getId())
                    .filter(userId -> !userId.equals(getCurrentUser().getId())) // Don't notify the creator
                    .collect(Collectors.toList());
            
            if (!memberUserIds.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("type", "MATCH_CREATED");
                data.put("matchId", savedMatch.getId());
                data.put("communityId", community.getId());
                
                pushNotificationService.sendPushNotificationToUsers(
                        memberUserIds.stream().map(UUID::toString).collect(Collectors.toList()),
                        "New Match Created",
                        "A new match \"" + savedMatch.getTitle() + "\" has been created in " + community.getName(),
                        data
                );
            }
        } catch (Exception e) {
            log.error("Error sending push notification for match creation", e);
            // Don't fail the match creation if notification fails
        }
        
        return savedMatch;
    }

    @Override
    @Transactional
    public Match updateMatchEvent(UUID id, UpdateMatchRequest request) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Match not found"));
        
        // Validate that user has COMMUNITY_MANAGER role for this match's community
        validateCommunityManagerAccess(match.getCommunity().getId());
        
        // Check if match can be updated (not in progress or completed)
        if (match.getStatus() == MatchStatus.IN_PROGRESS || match.getStatus() == MatchStatus.COMPLETED) {
            throw new BadRequestException("Cannot update match that is in progress or completed");
        }
        
        // Safely handle Optional fields
        if (request.getTitle().isPresent()) {
            match.setTitle(request.getTitle().orElse(match.getTitle()));
        }
        if (request.getDescription().isPresent()) {
            match.setDescription(request.getDescription().orElse(match.getDescription()));
        }
        if (request.getBannerUrl().isPresent()) {
            match.setBannerUrl(request.getBannerUrl().orElse(match.getBannerUrl()));
        }
        if (request.getMatchDate().isPresent()) {
            match.setMatchDate(request.getMatchDate().orElse(match.getMatchDate()));
        }
        if (request.getRegistrationDeadline().isPresent()) {
            match.setRegistrationDeadline(request.getRegistrationDeadline().orElse(match.getRegistrationDeadline()));
        }
        if (request.getPlayersPerTeam().isPresent()) {
            match.setPlayersPerTeam(request.getPlayersPerTeam().orElse(match.getPlayersPerTeam()));
        }
        if (request.getIsAutoSelection().isPresent()) {
            match.setAutoSelection(request.getIsAutoSelection().orElse(match.isAutoSelection()));
        }

        if (request.getType().isPresent()) {
            match.setType(request.getType().get().name());
        }

        if (request.getIsPaidEvent().isPresent()) {
            match.setIsPaidEvent(request.getIsPaidEvent().orElse(match.getIsPaidEvent()));
        }
        if (request.getPricePerPlayer().isPresent()) {
            match.setPricePerPlayer(request.getPricePerPlayer().orElse(match.getPricePerPlayer()));
        }
        if (request.getIsRefundable().isPresent()) {
            match.setIsRefundable(request.getIsRefundable().orElse(match.getIsRefundable()));
        }
        if (request.getMaxPlayers().isPresent()) {
            match.setMaxPlayers(request.getMaxPlayers().orElse(match.getMaxPlayers()));
        }
        if (request.getLatitude().isPresent()) {
            match.setLatitude(request.getLatitude().orElse(match.getLatitude()));
        }
        if (request.getLongitude().isPresent()) {
            match.setLongitude(request.getLongitude().orElse(match.getLongitude()));
        }
        if (request.getAddress().isPresent()) {
            match.setAddress(request.getAddress().orElse(match.getAddress()));
        }
        if (request.getCity().isPresent()) {
            match.setCity(request.getCity().orElse(match.getCity()));
        }
        if (request.getProvince().isPresent()) {
            match.setProvince(request.getProvince().orElse(match.getProvince()));
        }
        if (request.getPostCode().isPresent()) {
            match.setPostCode(request.getPostCode().orElse(match.getPostCode()));
        }
        if (request.getCountry().isPresent()) {
            match.setCountry(request.getCountry().orElse(match.getCountry()));
        }

        if (request.getType().isPresent()) {
            match.setType(request.getType().get().name());
        }

        return matchRepository.save(match);
    }

    @Override
    @Transactional
    public void deleteMatchEvent(UUID id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Match not found"));
        
        // Check if match can be deleted
        if (match.getStatus() == MatchStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot delete match that is in progress");
        }
        
        matchRepository.delete(match);
    }

    @Override
    public Match getMatchEventById(UUID id) {
        return matchRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Match not found"));
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
    public PaginatedResponse<MatchEventResponse> getMatchEvents(int page, int size, String sortBy, String sortDirection, String search) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
        );

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Match> matches;
        String searchFilter = buildContainsFilter(search);

        // If the current role is COMMUNITY_MANAGER, only show events created by that user.
        // Regular users (or when no role context) see all events.
        var currentUserRole = UserRoleContext.getCurrentUserRole();
        boolean isCommunityManager = currentUserRole != null
                && currentUserRole.isActive()
                && currentUserRole.getRole() != null
                && "COMMUNITY_MANAGER".equals(currentUserRole.getRole().getName());

        if (isCommunityManager) {
            User currentUser = getCurrentUser();
            matches = matchRepository.searchMatchesByCreator(currentUser.getId(), searchFilter, pageable);
        } else {
            if (searchFilter == null) {
                matches = matchRepository.findAll(pageable);
            } else {
                matches = matchRepository.searchMatches(searchFilter, pageable);
            }
        }

        Page<MatchEventResponse> responsePage = matches.map(MatchEventMapper::convertToResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    public PaginatedResponse<MatchEventResponse> getMatchEventsByCommunity(UUID communityId, int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
        );
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Match> matches;

        // For COMMUNITY_MANAGER role, only return matches in this community that they created.
        // Other roles see all matches for the community.
        var currentUserRole = UserRoleContext.getCurrentUserRole();
        boolean isCommunityManager = currentUserRole != null
                && currentUserRole.isActive()
                && currentUserRole.getRole() != null
                && "COMMUNITY_MANAGER".equals(currentUserRole.getRole().getName());
 
        

        if (isCommunityManager) {
            User currentUser = getCurrentUser();
            matches = matchRepository.findByCommunityIdAndCreatorId(communityId, currentUser.getId(), pageable);
        } else {
            matches = matchRepository.findByCommunityId(communityId, pageable);
        }
        
        Page<MatchEventResponse> responsePage = matches.map(MatchEventMapper::convertToResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    public PaginatedResponse<MatchEventResponse> getUpcomingMatchEvents(int page, int size, String search) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusMonths(1); // Next 3 months
        
        Sort sort = Sort.by(Sort.Direction.ASC, "matchDate");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        String searchFilter = buildContainsFilter(search);
        Page<Match> matches;

        UUID communityId = UserRoleContext.getCurrentCommunityId();
        UserRole roleContext = UserRoleContext.getCurrentUserRole();
        boolean isCommunityManager = roleContext != null
                && roleContext.isActive()
                && roleContext.getRole() != null
                && "COMMUNITY_MANAGER".equals(roleContext.getRole().getName());

        if (isCommunityManager) {
            User currentUser = getCurrentUser();
            // For community managers, fetch upcoming matches they created within this community
            matches = matchRepository.searchUpcomingMatchesByCommunityIdAndCreatorId(
                    communityId, currentUser.getId(), now, endDate, pageable);
        } else {
            if (searchFilter == null) {
                matches = matchRepository.findByDateRange(now, endDate, pageable);
            } else {
                matches = matchRepository.searchUpcomingMatches(now, endDate, searchFilter, pageable);
            }
        }
        Page<MatchEventResponse> responsePage = matches.map(MatchEventMapper::convertToResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    public PaginatedResponse<MatchEventResponse> getUserMatchEvents(int page, int size, String search) {
        User currentUser = getCurrentUser();
        
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        String searchFilter = buildContainsFilter(search);
        Page<Match> matches;
        if (searchFilter == null) {
            matches = matchRepository.findByUserId(currentUser.getId(), pageable);
        } else {
            matches = matchRepository.searchUserMatches(currentUser.getId(), searchFilter, pageable);
        }
        Page<MatchEventResponse> responsePage = matches.map(MatchEventMapper::convertToResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    @Transactional
    public void markAvailability(UUID matchId, AvailabilityRequest request) {
        User currentUser = getCurrentUser();
        Match match = getMatchEventById(matchId);
        
        // Check if registration is still open
        if (LocalDateTime.now().isAfter(match.getRegistrationDeadline())) {
            throw new BadRequestException("Registration deadline has passed");
        }
        
        // Check if user is a member of the community
        if (!isUserMemberOfCommunity(currentUser.getId(), match.getCommunity().getId())) {
            throw new BadRequestException("User is not a member of this community");
        }
        
        // Check if availability already exists
        Optional<Availability> existingAvailability = availabilityRepository.findByMatchIdAndUserId(matchId, currentUser.getId());
        
        if (existingAvailability.isPresent()) {
            // Update existing availability
            Availability availability = existingAvailability.get();
            if (request.getIsAvailable()) {
                availability.setStatus(AvailabilityStatus.AVAILABLE);
            } else {
                availability.setStatus(AvailabilityStatus.NOT_AVAILABLE);
            }
            availabilityRepository.save(availability);
        } else {
            // Create new availability
            Availability availability = Availability.builder()
                    .match(match)
                    .user(currentUser)
                    .status(request.getIsAvailable() ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.NOT_AVAILABLE)
                    .build();
            availabilityRepository.save(availability);
        }
    }

    @Override
    @Transactional
    public void removeAvailability(UUID matchId) {
        User currentUser = getCurrentUser();
        
        Availability availability = availabilityRepository.findByMatchIdAndUserId(matchId, currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Availability not found"));
        
        availabilityRepository.delete(availability);
    }

    @Override
    @Transactional
    public Match generateTeams(UUID matchId) {
        log.info("Generating teams for match: {}", matchId);
        Match match = getMatchEventById(matchId);

        log.info("Match: {}", match);
        
        // Check if teams can be generated
        if (match.getStatus() != MatchStatus.REGISTRATION_OPEN) {
            throw new BadRequestException("Teams can only be generated for upcoming matches");
        }

        
        // Get available players
        List<Availability> availablePlayers = availabilityRepository.findAvailablePlayers(matchId);
        
        if (availablePlayers.size() < match.getPlayersPerTeam() * 2) {
            throw new BadRequestException("Not enough available players to generate teams");
        }
        
        // Clear existing teams
        List<Team> existingTeams = teamRepository.findByMatchId(matchId);
        if (!existingTeams.isEmpty()) {
            teamRepository.deleteAll(existingTeams);
        }
        match.getTeams().clear();
        resetDraftState(match);
        
        // Generate teams
        if (match.isAutoSelection()) {
            log.info("Generating auto teams");
            generateAutoTeams(match, availablePlayers);
            match.setStatus(MatchStatus.TEAMS_SELECTED);
        } else {
            log.info("Generating manual teams");
            generateManualTeams(match, availablePlayers);
            match.setStatus(MatchStatus.TEAMS_MANUALLY_SELECTED);
        }
        return matchRepository.save(match);
    }

    @Override
    @Transactional
    public void selectPlayerForTeam(UUID matchId, UUID teamId, UUID userId) {
        Match match = getMatchEventById(matchId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BadRequestException("Team not found"));
        
        // Validate team belongs to match
        if (!team.getMatch().getId().equals(matchId)) {
            throw new BadRequestException("Team does not belong to this match");
        }
        
        if (!match.isAutoSelection()) {
            if (!Boolean.TRUE.equals(match.getDraftInProgress())) {
                throw new BadRequestException("Manual draft is not in progress for this match");
            }

            if (!teamId.equals(match.getCurrentPickingTeamId())) {
                throw new BadRequestException("It is not this team's turn to pick");
            }

            User currentUser = getCurrentUser();
            if (team.getCaptain() == null || !team.getCaptain().getId().equals(currentUser.getId())) {
                throw new BadRequestException("Only the current captain can select players during the draft");
            }
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        // Check if user is available
        Availability availability = availabilityRepository.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new BadRequestException("User has not marked availability for this match"));
        
        if (availability.getStatus() != AvailabilityStatus.AVAILABLE) {
            throw new BadRequestException("User is not available for this match");
        }
        
        // Check if team has space
        Long currentPlayers = teamPlayerRepository.countPlayersByTeamId(teamId);
        if (currentPlayers >= match.getPlayersPerTeam()) {
            throw new BadRequestException("Team is full");
        }
        
        // Check if user is already in a team for this match
        Optional<TeamPlayer> existingPlayer = teamPlayerRepository.findByMatchIdAndUserId(matchId, userId);
        if (existingPlayer.isPresent()) {
            throw new BadRequestException("User is already in a team for this match");
        }
        
        // Add player to team
        TeamPlayer teamPlayer = TeamPlayer.builder()
                .team(team)
                .user(user)
                .teamAvailabilityStatus(TeamAvailabilityStatus.SELECTED)
                .isCaptain(false)
                .build();
        
        teamPlayerRepository.save(teamPlayer);
        team.getPlayers().add(teamPlayer);

        availability.setStatus(AvailabilityStatus.SELECTED);
        availabilityRepository.save(availability);
        
        // Send push notification to the selected player
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "PLAYER_SELECTED");
            data.put("matchId", matchId);
            data.put("teamId", teamId);
            data.put("matchTitle", match.getTitle());
            
            pushNotificationService.sendPushNotificationToUser(
                    userId.toString(),
                    "You've Been Selected!",
                    "You have been selected for " + match.getTitle(),
                    data
            );
        } catch (Exception e) {
            log.error("Error sending push notification for player selection", e);
            // Don't fail the selection if notification fails
        }
        
        if (!match.isAutoSelection()) {
            advanceDraft(match, teamId);
        }

    }

    @Override
    @Transactional
    public void removePlayerFromTeam(UUID matchId, UUID teamId, UUID userId) {
        TeamPlayer teamPlayer = teamPlayerRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new BadRequestException("Player not found in team"));
        
        // Validate team belongs to match
        Match match = teamPlayer.getTeam().getMatch();
        if (!match.getId().equals(matchId)) {
            throw new BadRequestException("Team does not belong to this match");
        }
        
        // Store match title before deleting
        String matchTitle = match.getTitle();
        
        teamPlayerRepository.delete(teamPlayer);
        
        // Update availability status back to available
        Availability availability = availabilityRepository.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new BadRequestException("Availability not found"));
        
        availability.setStatus(AvailabilityStatus.AVAILABLE);
        availabilityRepository.save(availability);
        
        // Send push notification to the removed player
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "PLAYER_REMOVED");
            data.put("matchId", matchId);
            data.put("teamId", teamId);
            data.put("matchTitle", matchTitle);
            
            pushNotificationService.sendPushNotificationToUser(
                    userId.toString(),
                    "Removed from Team",
                    "You have been removed from the team for " + matchTitle,
                    data
            );
        } catch (Exception e) {
            log.error("Error sending push notification for player removal", e);
            // Don't fail the removal if notification fails
        }
    }

    @Override
    @Transactional
    public void assignCaptain(UUID teamId, UUID userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new BadRequestException("Team not found"));
        
        TeamPlayer teamPlayer = teamPlayerRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new BadRequestException("Player not found in team"));
        
        // Remove captain status from current captain
        Optional<TeamPlayer> currentCaptain = teamPlayerRepository.findCaptainByTeamId(teamId);
        if (currentCaptain.isPresent()) {
            currentCaptain.get().setCaptain(false);
            teamPlayerRepository.save(currentCaptain.get());
        }
        
        // Assign new captain
        teamPlayer.setCaptain(true);
        teamPlayerRepository.save(teamPlayer);
        
        // Update team captain
        team.setCaptain(teamPlayer.getUser());
        teamRepository.save(team);
        
        // Send push notification to the new captain
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "CAPTAIN_ASSIGNED");
            data.put("matchId", team.getMatch().getId());
            data.put("teamId", teamId);
            data.put("matchTitle", team.getMatch().getTitle());
            data.put("teamName", team.getName());
            
            pushNotificationService.sendPushNotificationToUser(
                    userId.toString(),
                    "You're Now a Captain!",
                    "You have been assigned as captain of " + team.getName() + " for " + team.getMatch().getTitle(),
                    data
            );
        } catch (Exception e) {
            log.error("Error sending push notification for captain assignment", e);
            // Don't fail the assignment if notification fails
        }
    }

    @Override
    @Transactional
    public void cancelMatch(UUID matchId) {
        Match match = getMatchEventById(matchId);
        
        if (match.getStatus() == MatchStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot cancel match that is in progress");
        }
        
        match.setStatus(MatchStatus.CANCELLED);
        matchRepository.save(match);
        
        // Send push notification to all players and community members
        try {
            List<UUID> userIds = new ArrayList<>();
            
            // Get all players in teams
            List<Team> teams = teamRepository.findByMatchId(matchId);
            for (Team team : teams) {
                List<TeamPlayer> players = teamPlayerRepository.findByTeamId(team.getId());
                for (TeamPlayer player : players) {
                    userIds.add(player.getUser().getId());
                }
            }
            
            // Get all users who marked availability
            List<Availability> availabilities = availabilityRepository.findByMatchIdWithUser(matchId);
            for (Availability availability : availabilities) {
                if (!userIds.contains(availability.getUser().getId())) {
                    userIds.add(availability.getUser().getId());
                }
            }
            
            if (!userIds.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("type", "MATCH_CANCELLED");
                data.put("matchId", matchId);
                
                pushNotificationService.sendPushNotificationToUsers(
                        userIds.stream().map(UUID::toString).collect(Collectors.toList()),
                        "Match Cancelled",
                        "The match \"" + match.getTitle() + "\" has been cancelled",
                        data
                );
            }
        } catch (Exception e) {
            log.error("Error sending push notification for match cancellation", e);
            // Don't fail the cancellation if notification fails
        }
    }

    @Override
    @Transactional
    public void startMatch(UUID matchId) {
        Match match = getMatchEventById(matchId);
        
        if (match.getStatus() != MatchStatus.TEAMS_SELECTED) {
            throw new BadRequestException("Match must have teams selected before starting");
        }
        
        match.setStatus(MatchStatus.IN_PROGRESS);
        matchRepository.save(match);
        
        // Send push notification to all players
        try {
            List<UUID> userIds = new ArrayList<>();
            List<Team> teams = teamRepository.findByMatchId(matchId);
            for (Team team : teams) {
                List<TeamPlayer> players = teamPlayerRepository.findByTeamId(team.getId());
                for (TeamPlayer player : players) {
                    userIds.add(player.getUser().getId());
                }
            }
            
            if (!userIds.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("type", "MATCH_STARTED");
                data.put("matchId", matchId);
                
                pushNotificationService.sendPushNotificationToUsers(
                        userIds.stream().map(UUID::toString).collect(Collectors.toList()),
                        "Match Started!",
                        "The match \"" + match.getTitle() + "\" has started",
                        data
                );
            }
        } catch (Exception e) {
            log.error("Error sending push notification for match start", e);
            // Don't fail the start if notification fails
        }
    }

    @Override
    @Transactional
    public void completeMatch(UUID matchId) {
        Match match = getMatchEventById(matchId);
        
        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new BadRequestException("Match must be in progress to complete");
        }
        
        match.setStatus(MatchStatus.COMPLETED);
        matchRepository.save(match);
        
        // Send push notification to all players
        try {
            List<UUID> userIds = new ArrayList<>();
            List<Team> teams = teamRepository.findByMatchId(matchId);
            for (Team team : teams) {
                List<TeamPlayer> players = teamPlayerRepository.findByTeamId(team.getId());
                for (TeamPlayer player : players) {
                    userIds.add(player.getUser().getId());
                }
            }
            
            if (!userIds.isEmpty()) {
                Map<String, Object> data = new HashMap<>();
                data.put("type", "MATCH_COMPLETED");
                data.put("matchId", matchId);
                
                pushNotificationService.sendPushNotificationToUsers(
                        userIds.stream().map(UUID::toString).collect(Collectors.toList()),
                        "Match Completed",
                        "The match \"" + match.getTitle() + "\" has been completed",
                        data
                );
            }
        } catch (Exception e) {
            log.error("Error sending push notification for match completion", e);
            // Don't fail the completion if notification fails
        }
    }

    @Override
    public List<String> getMatchEventsMetadata() {
        // return all match ids
        List<Match> matches = matchRepository.findAll();
        return matches.stream()
                .map(match -> match.getId().toString())
                .collect(Collectors.toList());
    }

    private void generateAutoTeams(Match match, List<Availability> availablePlayers) {
        // Shuffle players for random assignment
        Collections.shuffle(availablePlayers);
        
        int playersPerTeam = match.getPlayersPerTeam();
        int numTeams = availablePlayers.size() / playersPerTeam;
        
        // Create teams
        for (int i = 0; i < numTeams; i++) {
            Team team = Team.builder()
                    .match(match)
                    .name("Team " + (char)('A' + i))
                    .color(getTeamColor(i))
                    .build();

            match.getTeams().add(team);
            Team savedTeam = teamRepository.save(team);

            User teamCaptain = null;

            // Assign players to team
            for (int j = 0; j < playersPerTeam; j++) {
                int playerIndex = i * playersPerTeam + j;
                if (playerIndex < availablePlayers.size()) {
                    Availability availability = availablePlayers.get(playerIndex);

                    if (teamCaptain == null) {
                        teamCaptain = availability.getUser();
                    }

                    TeamPlayer teamPlayer = TeamPlayer.builder()
                            .team(savedTeam)
                            .user(availability.getUser())
                            .teamAvailabilityStatus(TeamAvailabilityStatus.SELECTED)
                            .isCaptain(j == 0) // First player is captain
                            .build();

                    teamPlayerRepository.save(teamPlayer);
                    savedTeam.getPlayers().add(teamPlayer);
                    availability.setStatus(AvailabilityStatus.SELECTED);
                    availabilityRepository.save(availability);
                }
            }

            if (teamCaptain != null) {
                savedTeam.setCaptain(teamCaptain);
                teamRepository.save(savedTeam);
            }
        }
        
        // Handle remaining players as reserves
        int remainingPlayers = availablePlayers.size() % playersPerTeam;
        if (remainingPlayers > 0) {
            Team reserveTeam = Team.builder()
                    .match(match)
                    .name("Reserve Team")
                    .color(getTeamColor(numTeams))
                    .build();

            match.getTeams().add(reserveTeam);
            Team savedReserveTeam = teamRepository.save(reserveTeam);

            for (int i = availablePlayers.size() - remainingPlayers; i < availablePlayers.size(); i++) {
                Availability availability = availablePlayers.get(i);

                TeamPlayer teamPlayer = TeamPlayer.builder()
                        .team(savedReserveTeam)
                        .user(availability.getUser())
                        .teamAvailabilityStatus(TeamAvailabilityStatus.RESERVE)
                        .isCaptain(false)
                        .build();

                teamPlayerRepository.save(teamPlayer);
                savedReserveTeam.getPlayers().add(teamPlayer);
                availability.setStatus(AvailabilityStatus.RESERVE);
                availabilityRepository.save(availability);
            }
        }
    }

    private void generateManualTeams(Match match, List<Availability> availablePlayers) {
        int playersPerTeam = match.getPlayersPerTeam();
        int numTeams = (int) Math.floor((double) availablePlayers.size() / playersPerTeam);

        if (numTeams < 2) {
            throw new BadRequestException("Manual drafting requires at least two teams");
        }

        Random random = new Random();
        List<UUID> draftOrder = new ArrayList<>();

        for (int i = 0; i < numTeams; i++) {
            Team team = Team.builder()
                    .match(match)
                    .name("Team " + (char)('A' + i))
                    .color(getTeamColor(i))
                    .build();

            Team savedTeam = teamRepository.save(team);
            match.getTeams().add(savedTeam);
            draftOrder.add(savedTeam.getId());

            if (availablePlayers.isEmpty()) {
                throw new BadRequestException("Not enough players to assign captains");
            }

            int randomIndex = random.nextInt(availablePlayers.size());
            Availability captainAvailability = availablePlayers.remove(randomIndex);
            User captain = captainAvailability.getUser();

            TeamPlayer teamPlayer = TeamPlayer.builder()
                    .team(savedTeam)
                    .user(captain)
                    .teamAvailabilityStatus(TeamAvailabilityStatus.SELECTED)
                    .isCaptain(true)
                    .build();
            teamPlayerRepository.save(teamPlayer);
            savedTeam.getPlayers().add(teamPlayer);
            savedTeam.setCaptain(captain);
            teamRepository.save(savedTeam);

            captainAvailability.setStatus(AvailabilityStatus.SELECTED);
            availabilityRepository.save(captainAvailability);
        }

        if (!draftOrder.isEmpty()) {
            match.setManualDraftOrder(draftOrder.stream().map(UUID::toString).collect(Collectors.joining(",")));
            match.setManualDraftIndex(0);
            Team firstTeam = teamRepository.findById(draftOrder.get(0))
                    .orElseThrow(() -> new BadRequestException("Team not found"));
            match.setCurrentPickingTeam(firstTeam);
            match.setDraftInProgress(true);
        } else {
            resetDraftState(match);
        }
    }

    private void advanceDraft(Match match, UUID justPickedTeamId) {
        List<UUID> draftOrder = parseDraftOrder(match);

        if (draftOrder.isEmpty() || match.getPlayersPerTeam() == null) {
            resetDraftState(match);
            matchRepository.save(match);
            return;
        }

        int currentIndex = match.getManualDraftIndex() == null ? 0 : match.getManualDraftIndex();
        if (currentIndex < 0 || currentIndex >= draftOrder.size() || !Objects.equals(draftOrder.get(currentIndex), justPickedTeamId)) {
            currentIndex = draftOrder.indexOf(justPickedTeamId);
        }
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        int playersPerTeam = match.getPlayersPerTeam();
        int nextIndex = -1;

        for (int offset = 1; offset <= draftOrder.size(); offset++) {
            int candidateIndex = (currentIndex + offset) % draftOrder.size();
            UUID candidateTeamId = draftOrder.get(candidateIndex);
            Long count = teamPlayerRepository.countPlayersByTeamId(candidateTeamId);
            if (count < playersPerTeam) {
                nextIndex = candidateIndex;
                break;
            }
        }

        if (nextIndex == -1) {
            match.setDraftInProgress(false);
            match.setCurrentPickingTeam(null);
            match.setManualDraftIndex(0);
            match.setStatus(MatchStatus.TEAMS_SELECTED);
            matchRepository.save(match);
            assignReserveTeam(match);
        } else {
            match.setManualDraftIndex(nextIndex);
            Team nextTeam = teamRepository.findById(draftOrder.get(nextIndex))
                    .orElseThrow(() -> new BadRequestException("Team not found"));
            match.setCurrentPickingTeam(nextTeam);
            matchRepository.save(match);
        }
    }

    private List<UUID> parseDraftOrder(Match match) {
        String draftOrder = match.getManualDraftOrder();
        if (draftOrder == null || draftOrder.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(draftOrder.split(","))
                .filter(token -> !token.isBlank())
                .map(String::trim)
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    private void assignReserveTeam(Match match) {
        List<Availability> remainingAvailabilities = availabilityRepository.findAvailablePlayers(match.getId());

        if (remainingAvailabilities.isEmpty()) {
            return;
        }

        Team reserveTeam = teamRepository.findByMatchIdAndName(match.getId(), "Reserve Team")
                .orElseGet(() -> {
                    Team newReserveTeam = Team.builder()
                            .match(match)
                            .name("Reserve Team")
                            .color(getTeamColor(match.getTeams().size()))
                            .build();
                    Team saved = teamRepository.save(newReserveTeam);
                    if (match.getTeams().stream().noneMatch(team -> team.getId().equals(saved.getId()))) {
                        match.getTeams().add(saved);
                    }
                    return saved;
                });

        List<TeamPlayer> existingReservePlayers = teamPlayerRepository.findByTeamId(reserveTeam.getId());
        if (!existingReservePlayers.isEmpty()) {
            teamPlayerRepository.deleteAll(existingReservePlayers);
            reserveTeam.getPlayers().clear();
        }

        for (Availability availability : remainingAvailabilities) {
            TeamPlayer reservePlayer = TeamPlayer.builder()
                    .team(reserveTeam)
                    .user(availability.getUser())
                    .teamAvailabilityStatus(TeamAvailabilityStatus.RESERVE)
                    .isCaptain(false)
                    .build();
            teamPlayerRepository.save(reservePlayer);
            reserveTeam.getPlayers().add(reservePlayer);

            availability.setStatus(AvailabilityStatus.RESERVE);
            availabilityRepository.save(availability);
        }

        teamRepository.save(reserveTeam);
        matchRepository.save(match);
    }

    private void resetDraftState(Match match) {
        match.setDraftInProgress(false);
        match.setCurrentPickingTeam(null);
        match.setManualDraftOrder(null);
        match.setManualDraftIndex(0);
    }

    private String getTeamColor(int teamIndex) {
        String[] colors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#F7CAC9", "#F7819F", "#92A8D1", "#A2D2FF", "#B1E19B", "#FFFFC7", "#F7A35C", "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#F7CAC9", "#F7819F", "#92A8D1", "#A2D2FF", "#B1E19B", "#FFFFC7", "#F7A35C"};
        return colors[teamIndex % colors.length];
    }

    private boolean isUserMemberOfCommunity(UUID userId, UUID communityId) {
        if (userId == null || communityId == null) {
            return false;
        }

        // Delegate to CommunityMemberRepository for an efficient existence check
        return communityMemberRepository.existsByCommunityIdAndUserIdAndIsActiveTrue(communityId, userId);
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
        org.springframework.security.core.Authentication authentication = 
                SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            throw new BadRequestException("Authentication required. Please provide a valid access token.");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal == null) {
            throw new BadRequestException("Invalid authentication: principal is null");
        }
        
        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }
        
        throw new BadRequestException("Invalid authentication principal type: " + 
                (principal != null ? principal.getClass().getName() : "null"));
    }

    @Override
    @Transactional
    public void addAvailability(UUID matchId, AvailabilityRequest request) {
        Match match = getMatchEventById(matchId);
        User currentUser = getCurrentUser();

        // Check if registration is still open
        if (LocalDateTime.now().isAfter(match.getRegistrationDeadline())) {
            throw new BadRequestException("Registration deadline has passed");
        }
        
        // Check if user is a member of the community
        if (!isUserMemberOfCommunity(currentUser.getId(), match.getCommunity().getId())) {
            // Add user to the community
            CommunityMember communityMember = CommunityMember.builder()
                    .community(match.getCommunity())
                    .user(currentUser)
                    .isActive(true)
                    .build();
            communityMemberRepository.save(communityMember);
        }

        // Check if the person is around 0.5 mile radius of the community using the longitude and latitude of the community
        if (!isUserInRadius(currentUser.getId(), match.getCommunity().getLongitude(), match.getCommunity().getLatitude())) {
            throw new BadRequestException("Please be within one mile radius of the community to mark availability");
        }
        
        // Create new availability
        Availability availability = Availability.builder()
                .match(match)
                .user(currentUser)
                .status(AvailabilityStatus.AVAILABLE)
                .build();
        availabilityRepository.save(availability);

        // Update match status
        match.setStatus(MatchStatus.REGISTRATION_OPEN);
        matchRepository.save(match);

        // Send notification to the community members
       
    }

    @Override
    @Transactional
    public void joinMatchEvent(UUID matchId, JoinMatchRequest request) {
        Match match = getMatchEventById(matchId);
        User currentUser = getCurrentUser();

        // Check match status and registration deadline
        if (LocalDateTime.now().isAfter(match.getRegistrationDeadline())) {
            throw new BadRequestException("Registration deadline has passed");
        }
        if (match.getStatus() == MatchStatus.CANCELLED || match.getStatus() == MatchStatus.COMPLETED) {
            throw new BadRequestException("Cannot join a cancelled or completed match");
        }

        // Optional: ensure the user is within the allowed radius of the community
        if (!isUserInRadius(currentUser.getId(), match.getCommunity().getLongitude(), match.getCommunity().getLatitude())) {
            throw new BadRequestException("Please be within one mile radius of the community to join this match");
        }

        // Prevent duplicate joins (separate from availability)
        if (matchRegistrationRepository.existsByMatchIdAndUserId(matchId, currentUser.getId())) {
            throw new BadRequestException("You have already joined this match");
        }

        boolean isPaidEvent = Boolean.TRUE.equals(match.getIsPaidEvent())
                && match.getPricePerPlayer() != null
                && match.getPricePerPlayer().compareTo(BigDecimal.ZERO) > 0;

        Payment payment = null;
        if (isPaidEvent) {
            if (request == null || request.getPaymentIntentId() == null || request.getPaymentIntentId().isBlank()) {
                throw new BadRequestException("A confirmed payment is required to join this paid match");
            }

            BigDecimal expectedAmount = match.getPricePerPlayer();

            // Lookup the existing confirmed payment by its intent ID
            payment = paymentRepository.findByStripePaymentIntentId(request.getPaymentIntentId())
                    .orElseThrow(() -> new BadRequestException("Payment not found for the provided payment intent"));

            if (!payment.getUser().getId().equals(currentUser.getId())) {
                throw new BadRequestException("Payment does not belong to the current user");
            }


            if (payment.getType() != PaymentType.MATCH_FEE) {
                throw new BadRequestException("Payment is not a match fee");
            }

            if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
                throw new BadRequestException("Payment has not been confirmed");
            }

            if (payment.getAmount() == null || payment.getAmount().compareTo(expectedAmount) != 0) {
                throw new BadRequestException("Payment amount does not match the required match fee");
            }

            // Link the confirmed payment to this match (if not already linked)
            if (payment.getMatch() == null) {
                payment.setMatch(match);
                payment = paymentRepository.save(payment);
            } else if (!payment.getMatch().getId().equals(match.getId())) {
                throw new BadRequestException("Payment is already linked to a different match");
            }
        }

        // At this point all validations (and payment for paid matches) have succeeded.
        // Now ensure the user is (or becomes) a member of the community.
        if (!isUserMemberOfCommunity(currentUser.getId(), match.getCommunity().getId())) {
            CommunityMember communityMember = CommunityMember.builder()
                    .community(match.getCommunity())
                    .user(currentUser)
                    .isActive(true)
                    .build();
            communityMemberRepository.save(communityMember);
        }

        // Create a dedicated registration record (joining the match)
        MatchRegistration registration = MatchRegistration.builder()
                .match(match)
                .user(currentUser)
                .payment(payment)
                .build();
        matchRegistrationRepository.save(registration);

        // Ensure the match is in registration open state once players start joining
        if (match.getStatus() == MatchStatus.UPCOMING) {
            match.setStatus(MatchStatus.REGISTRATION_OPEN);
            matchRepository.save(match);
        }

        // Add player to availability
        Availability availability = Availability.builder()
                .match(match)
                .user(currentUser)
                .status(AvailabilityStatus.NOT_AVAILABLE)
                .build();
        availabilityRepository.save(availability);

        // Send push notification to all players that the user has joined the match
        List<User> players = matchRegistrationRepository.findUsersByMatchId(matchId);
        if (!players.isEmpty()) {
            String joiningUserName = (currentUser.getFirstName() != null ? currentUser.getFirstName() : "") +
                    (currentUser.getLastName() != null ? " " + currentUser.getLastName() : "");

            Map<String, Object> data = new HashMap<>();
            data.put("type", "MATCH_JOINED");
            data.put("matchId", matchId);
            data.put("joiningUserId", currentUser.getId());

            for (User player : players) {
                pushNotificationService.sendPushNotificationToUser(
                        player.getId().toString(),
                        "Player Joined Match",
                        joiningUserName.trim() + " has joined " + match.getTitle(),
                        data
                );
            }
        }
    }

    private boolean isUserInRadius(UUID userId, BigDecimal longitude, BigDecimal latitude) {
        // This would need to be implemented based on your location service logic
        // For now, return true - you can implement proper location check
        return true;
    }

    
}
