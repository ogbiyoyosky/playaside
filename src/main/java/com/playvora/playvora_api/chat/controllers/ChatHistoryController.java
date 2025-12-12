package com.playvora.playvora_api.chat.controllers;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.chat.dtos.ActiveChatSummaryResponse;
import com.playvora.playvora_api.chat.enums.ChatType;
import com.playvora.playvora_api.chat.dtos.CommunityChatMessageResponse;
import com.playvora.playvora_api.chat.dtos.PrivateChatMessageResponse;
import com.playvora.playvora_api.chat.entities.CommunityChatMessage;
import com.playvora.playvora_api.chat.entities.CommunityChatReadState;
import com.playvora.playvora_api.chat.entities.PrivateChatMessage;
import com.playvora.playvora_api.chat.entities.PrivateChatReadState;
import com.playvora.playvora_api.chat.repo.CommunityChatMessageRepository;
import com.playvora.playvora_api.chat.repo.CommunityChatReadStateRepository;
import com.playvora.playvora_api.chat.repo.PrivateChatMessageRepository;
import com.playvora.playvora_api.chat.repo.PrivateChatReadStateRepository;
import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.community.repo.CommunityMemberRepository;
import com.playvora.playvora_api.community.repo.CommunityRepository;
import com.playvora.playvora_api.match.dtos.chat.ChatMessageResponse;
import com.playvora.playvora_api.match.entities.ChatMessage;
import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.match.entities.MatchChatReadState;
import com.playvora.playvora_api.match.entities.MatchRegistration;
import com.playvora.playvora_api.match.repo.ChatMessageRepository;
import com.playvora.playvora_api.match.repo.MatchChatReadStateRepository;
import com.playvora.playvora_api.match.repo.MatchRegistrationRepository;
import com.playvora.playvora_api.match.repo.MatchRepository;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat History", description = "REST endpoints for chat history (private, community, and match events)")
@RequestMapping("/api/v1/chat")
public class ChatHistoryController {

    private final UserRepository userRepository;
    private final PrivateChatMessageRepository privateChatMessageRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityRepository communityRepository;
    private final PrivateChatReadStateRepository privateChatReadStateRepository;
    private final CommunityChatReadStateRepository communityChatReadStateRepository;
    private final MatchRepository matchRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MatchRegistrationRepository matchRegistrationRepository;
    private final MatchChatReadStateRepository matchChatReadStateRepository;

    @GetMapping("/active")
    @Operation(
            summary = "Get active chats",
            description = "Get a paginated list of active chats (private, community, and match events) " +
                          "for the current user, ordered by most recent activity. Optional filter by chat type."
    )
    public ResponseEntity<ApiResponse<PaginatedResponse<ActiveChatSummaryResponse>>> getActiveChats(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by chat type") @RequestParam(required = false) ChatType chatType
    ) {
        try {
            if (page < 0 || size <= 0) {
                throw new BadRequestException("Invalid pagination parameters");
            }

            User currentUser = getCurrentUser();

            List<ActiveChatSummaryResponse> allChats = new ArrayList<>();

            // 1) Private chats
            allChats.addAll(buildPrivateChatSummaries(currentUser));

            // 2) Community chats
            allChats.addAll(buildCommunityChatSummaries(currentUser));

            // 3) Match/event chats
            allChats.addAll(buildMatchChatSummaries(currentUser));

            // Filter by chat type if provided
            if (chatType != null) {
                allChats = allChats.stream()
                        .filter(chat -> chatType.equals(chat.getChatType()))
                        .collect(java.util.stream.Collectors.toList());
            }

            // Sort by lastMessageAt DESC
            allChats.sort(Comparator.comparing(ActiveChatSummaryResponse::getLastMessageAt).reversed());

            // Manual pagination on the aggregated list
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, allChats.size());

            List<ActiveChatSummaryResponse> pageRecords = new ArrayList<>();
            if (fromIndex < allChats.size()) {
                pageRecords = allChats.subList(fromIndex, toIndex);
            }

            int totalPages = (int) Math.ceil(allChats.size() / (double) size);
            Integer prevPage = page > 0 ? page - 1 : null;
            Integer nextPage = page < totalPages - 1 ? page + 1 : null;

            PaginatedResponse<ActiveChatSummaryResponse> paginated = PaginatedResponse.<ActiveChatSummaryResponse>builder()
                    .records(pageRecords)
                    .count(allChats.size())
                    .totalPages(totalPages)
                    .currentPage(page)
                    .prevPage(prevPage)
                    .nextPage(nextPage)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(paginated, "Active chats retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching active chats: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch active chats: " + e.getMessage()));
        }
    }

    @GetMapping("/private/{userId}")
    @Operation(summary = "Get private chat history",
            description = "Get paginated chat history between the current user and another user")
    public ResponseEntity<ApiResponse<PaginatedResponse<PrivateChatMessageResponse>>> getPrivateChatHistory(
            @Parameter(description = "Other user ID") @PathVariable UUID userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        try {
            User currentUser = getCurrentUser();

            if (currentUser.getId().equals(userId)) {
                throw new BadRequestException("Cannot fetch private chat history with yourself");
            }

            // Ensure other user exists (optional but nicer error)
            userRepository.findById(userId)
                    .orElseThrow(() -> new BadRequestException("User not found"));

            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));
            Page<PrivateChatMessage> messages =
                    privateChatMessageRepository.findConversation(currentUser.getId(), userId, pageable);

            Page<PrivateChatMessageResponse> responsePage = messages.map(msg -> {
                User sender = msg.getSender();
                User recipient = msg.getRecipient();
                String senderName = sender.getFirstName() + " " + sender.getLastName();
                String recipientName = recipient.getFirstName() + " " + recipient.getLastName();

                String conversationId = buildConversationId(sender.getId(), recipient.getId());

                return PrivateChatMessageResponse.builder()
                        .id(msg.getId())
                        .senderId(sender.getId())
                        .senderName(senderName)
                        .recipientId(recipient.getId())
                        .recipientName(recipientName)
                        .message(msg.getMessage())
                        .createdAt(msg.getCreatedAt())
                        .conversationId(conversationId)
                        .build();
            });

            PaginatedResponse<PrivateChatMessageResponse> paginatedResponse =
                    PaginationUtils.toPaginatedResponse(responsePage);

            // Compute unread count for current user in this conversation
            PrivateChatReadState readState = privateChatReadStateRepository
                    .findByUserIdAndOtherUserId(currentUser.getId(), userId)
                    .orElse(null);
            OffsetDateTime lastReadAt = readState != null ? readState.getLastReadAt() : null;
            long unreadCount = (lastReadAt == null)
                    ? privateChatMessageRepository
                            .countUnreadForRecipientInConversation(currentUser.getId(), userId)
                    : privateChatMessageRepository
                            .countUnreadForRecipientInConversationSince(currentUser.getId(), userId, lastReadAt);
            paginatedResponse.setUnReadCount(unreadCount);

            return ResponseEntity.ok(ApiResponse.success(paginatedResponse, "Private chat history retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching private chat history: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch private chat history: " + e.getMessage()));
        }
    }

    @GetMapping("/communities/{communityId}")
    @Operation(summary = "Get community chat history",
            description = "Get paginated chat history for a community (only members can access)")
    public ResponseEntity<ApiResponse<PaginatedResponse<CommunityChatMessageResponse>>> getCommunityChatHistory(
            @Parameter(description = "Community ID") @PathVariable UUID communityId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        try {
            User currentUser = getCurrentUser();

            boolean isMember = communityMemberRepository
                    .existsByCommunityIdAndUserIdAndIsActiveTrue(communityId, currentUser.getId());
            if (!isMember) {
                throw new BadRequestException("You must be a member of this community to view chat messages");
            }

            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));
            Page<CommunityChatMessage> messages =
                    communityChatMessageRepository.findByCommunityIdOrderByCreatedAtAsc(communityId, pageable);

            Page<CommunityChatMessageResponse> responsePage = messages.map(msg -> {
                User sender = msg.getSender();
                String senderName = sender.getFirstName() + " " + sender.getLastName();
                return CommunityChatMessageResponse.builder()
                        .id(msg.getId())
                        .communityId(communityId)
                        .senderId(sender.getId())
                        .senderName(senderName)
                        .message(msg.getMessage())
                        .createdAt(msg.getCreatedAt())
                        .build();
            });

            PaginatedResponse<CommunityChatMessageResponse> paginatedResponse =
                    PaginationUtils.toPaginatedResponse(responsePage);

            // Compute unread count for current user in this community
            CommunityChatReadState readState = communityChatReadStateRepository
                    .findByUserIdAndCommunityId(currentUser.getId(), communityId)
                    .orElse(null);
            OffsetDateTime lastReadAt = readState != null ? readState.getLastReadAt() : null;
            long unreadCount = (lastReadAt == null)
                    ? communityChatMessageRepository
                            .countUnreadForUserInCommunity(communityId, currentUser.getId())
                    : communityChatMessageRepository
                            .countUnreadForUserInCommunitySince(communityId, currentUser.getId(), lastReadAt);
            paginatedResponse.setUnReadCount(unreadCount);

            return ResponseEntity.ok(ApiResponse.success(paginatedResponse, "Community chat history retrieved successfully"));
        } catch (Exception e) {
            log.error("Error fetching community chat history: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch community chat history: " + e.getMessage()));
        }
    }

    @PostMapping("/private/{userId}/mark-read")
    @Operation(summary = "Mark private chat as read",
            description = "Mark all messages in a private conversation as read for the current user")
    public ResponseEntity<ApiResponse<Void>> markPrivateChatAsRead(
            @Parameter(description = "Other user ID") @PathVariable UUID userId
    ) {
        User currentUser = getCurrentUser();

        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("Cannot mark private chat with yourself as read");
        }

        PrivateChatReadState state = privateChatReadStateRepository
                .findByUserIdAndOtherUserId(currentUser.getId(), userId)
                .orElse(PrivateChatReadState.builder()
                        .user(currentUser)
                        .otherUser(userRepository.findById(userId)
                                .orElseThrow(() -> new BadRequestException("User not found")))
                        .build());

        state.setLastReadAt(OffsetDateTime.now());
        privateChatReadStateRepository.save(state);

        return ResponseEntity.ok(ApiResponse.success(null, "Private chat marked as read"));
    }

    @PostMapping("/communities/{communityId}/mark-read")
    @Operation(summary = "Mark community chat as read",
            description = "Mark all messages in a community as read for the current user")
    public ResponseEntity<ApiResponse<Void>> markCommunityChatAsRead(
            @Parameter(description = "Community ID") @PathVariable UUID communityId
    ) {
        User currentUser = getCurrentUser();

        boolean isMember = communityMemberRepository
                .existsByCommunityIdAndUserIdAndIsActiveTrue(communityId, currentUser.getId());
        if (!isMember) {
            throw new BadRequestException("You must be a member of this community to mark messages as read");
        }

        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new BadRequestException("Community not found"));

        CommunityChatReadState state = communityChatReadStateRepository
                .findByUserIdAndCommunityId(currentUser.getId(), communityId)
                .orElse(CommunityChatReadState.builder()
                        .user(currentUser)
                        .community(community)
                        .build());

        state.setLastReadAt(OffsetDateTime.now());
        communityChatReadStateRepository.save(state);

        return ResponseEntity.ok(ApiResponse.success(null, "Community chat marked as read"));
    }

    @GetMapping("/match-events/{matchId}")
    @Operation(summary = "Get match chat history", description = "Get chat messages for a match event with pagination")
    public ResponseEntity<ApiResponse<PaginatedResponse<ChatMessageResponse>>> getMatchChatHistory(
            @Parameter(description = "Match ID") @PathVariable UUID matchId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

        try {
            User currentUser = getCurrentUser();

            // Verify match exists
            matchRepository.findById(matchId)
                    .orElseThrow(() -> new BadRequestException("Match not found"));

            // Verify user is a participant in this match (has registered)
            boolean isParticipant = matchRegistrationRepository.existsByMatchIdAndUserId(matchId, currentUser.getId());
            if (!isParticipant) {
                throw new BadRequestException("You must be a participant in this match to view chat messages");
            }

            // Fetch chat messages with pagination (newest first)
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<ChatMessage> chatMessages = chatMessageRepository.findByMatchIdOrderByCreatedAtDesc(matchId, pageable);

            // Convert to response DTOs
            Page<ChatMessageResponse> responsePage = chatMessages.map(chatMessage -> {
                User sender = chatMessage.getSender();
                return ChatMessageResponse.builder()
                        .id(chatMessage.getId())
                        .matchId(matchId)
                        .senderId(sender.getId())
                        .senderName(sender.getFirstName() + " " + sender.getLastName())
                        .senderFirstName(sender.getFirstName())
                        .senderLastName(sender.getLastName())
                        .message(chatMessage.getMessage())
                        .createdAt(chatMessage.getCreatedAt())
                        .build();
            });

            PaginatedResponse<ChatMessageResponse> paginatedResponse = PaginationUtils.toPaginatedResponse(responsePage);

            // Compute unread count for current user in this match
            MatchChatReadState readState = matchChatReadStateRepository
                    .findByUserIdAndMatchId(currentUser.getId(), matchId)
                    .orElse(null);
            OffsetDateTime lastReadAt = readState != null ? readState.getLastReadAt() : null;
            long unreadCount = (lastReadAt == null)
                    ? chatMessageRepository
                            .countUnreadForUserInMatch(matchId, currentUser.getId())
                    : chatMessageRepository
                            .countUnreadForUserInMatchSince(matchId, currentUser.getId(), lastReadAt);
            paginatedResponse.setUnReadCount(unreadCount);

            return ResponseEntity.ok(ApiResponse.success(paginatedResponse, "Chat history retrieved successfully"));

        } catch (Exception e) {
            log.error("Error fetching match chat history: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch chat history: " + e.getMessage()));
        }
    }

    @PostMapping("/match-events/{matchId}/mark-read")
    @Operation(summary = "Mark match chat as read",
            description = "Mark all messages in a match chat as read for the current user")
    public ResponseEntity<ApiResponse<Void>> markMatchChatAsRead(
            @Parameter(description = "Match ID") @PathVariable UUID matchId
    ) {
        User currentUser = getCurrentUser();

        // Verify match exists and user participates
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BadRequestException("Match not found"));
        boolean isParticipant = matchRegistrationRepository.existsByMatchIdAndUserId(matchId, currentUser.getId());
        if (!isParticipant) {
            throw new BadRequestException("You must be a participant in this match to mark messages as read");
        }

        MatchChatReadState state = matchChatReadStateRepository
                .findByUserIdAndMatchId(currentUser.getId(), matchId)
                .orElse(MatchChatReadState.builder()
                        .user(currentUser)
                        .match(match)
                        .build());

        state.setLastReadAt(OffsetDateTime.now());
        matchChatReadStateRepository.save(state);

        return ResponseEntity.ok(ApiResponse.success(null, "Match chat marked as read"));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }

        throw new BadRequestException("Invalid authentication principal");
    }

    /**
     * Build summaries for private chats the user participates in.
     */
    private List<ActiveChatSummaryResponse> buildPrivateChatSummaries(User currentUser) {
        List<PrivateChatMessage> recentMessages =
                privateChatMessageRepository.findTop500BySenderIdOrRecipientIdOrderByCreatedAtDesc(
                        currentUser.getId(), currentUser.getId());

        Map<String, PrivateChatMessage> latestByConversation = new HashMap<>();

        for (PrivateChatMessage message : recentMessages) {
            UUID senderId = message.getSender().getId();
            UUID recipientId = message.getRecipient().getId();

            String conversationKey = buildConversationId(senderId, recipientId);

            // Since messages are already ordered DESC, the first one we see is the latest
            latestByConversation.putIfAbsent(conversationKey, message);
        }

        List<ActiveChatSummaryResponse> result = new ArrayList<>();

        for (PrivateChatMessage msg : latestByConversation.values()) {
            User otherUser = msg.getSender().getId().equals(currentUser.getId())
                    ? msg.getRecipient()
                    : msg.getSender();

            // Unread count for this conversation
            PrivateChatReadState readState = privateChatReadStateRepository
                    .findByUserIdAndOtherUserId(currentUser.getId(), otherUser.getId())
                    .orElse(null);
            OffsetDateTime lastReadAt = readState != null ? readState.getLastReadAt() : null;
            long unread = (lastReadAt == null)
                    ? privateChatMessageRepository
                            .countUnreadForRecipientInConversation(currentUser.getId(), otherUser.getId())
                    : privateChatMessageRepository
                            .countUnreadForRecipientInConversationSince(currentUser.getId(), otherUser.getId(), lastReadAt);

            String title = otherUser.getFirstName() + " " + otherUser.getLastName();
            String imageUrl = otherUser.getProfilePictureUrl();

            result.add(ActiveChatSummaryResponse.builder()
                    .chatType(ChatType.PRIVATE)
                    .targetId(otherUser.getId())
                    .title(title)
                    .subtitle(null)
                    .imageUrl(imageUrl)
                    .lastMessage(msg.getMessage())
                    .lastMessageAt(msg.getCreatedAt())
                    .unreadCount(unread)
                    .build());
        }

        return result;
    }

    /**
     * Build summaries for community chats where the user is an active member.
     */
    private List<ActiveChatSummaryResponse> buildCommunityChatSummaries(User currentUser) {
        List<ActiveChatSummaryResponse> result = new ArrayList<>();

        var memberships = communityMemberRepository.findActiveMembershipsByUserId(currentUser.getId());

        for (var membership : memberships) {
            var community = membership.getCommunity();

            CommunityChatMessage lastMessage =
                    communityChatMessageRepository.findTop1ByCommunityIdOrderByCreatedAtDesc(community.getId());

            if (lastMessage == null) {
                // No messages yet in this community
                continue;
            }

            CommunityChatReadState readState = communityChatReadStateRepository
                    .findByUserIdAndCommunityId(currentUser.getId(), community.getId())
                    .orElse(null);
            OffsetDateTime lastReadAt = readState != null ? readState.getLastReadAt() : null;
            long unread = (lastReadAt == null)
                    ? communityChatMessageRepository
                            .countUnreadForUserInCommunity(community.getId(), currentUser.getId())
                    : communityChatMessageRepository
                            .countUnreadForUserInCommunitySince(community.getId(), currentUser.getId(), lastReadAt);

            result.add(ActiveChatSummaryResponse.builder()
                    .chatType(ChatType.COMMUNITY)
                    .targetId(community.getId())
                    .title(community.getName())
                    .subtitle(null)
                    .imageUrl(community.getBannerUrl())
                    .lastMessage(lastMessage.getMessage())
                    .lastMessageAt(lastMessage.getCreatedAt())
                    .unreadCount(unread)
                    .build());
        }

        return result;
    }

    /**
     * Build summaries for match/event chats where the user is registered.
     */
    private List<ActiveChatSummaryResponse> buildMatchChatSummaries(User currentUser) {
        List<ActiveChatSummaryResponse> result = new ArrayList<>();

        var registrations = matchRegistrationRepository.findByUserId(currentUser.getId());

        for (MatchRegistration registration : registrations) {
            Match match = registration.getMatch();

            ChatMessage lastMessage = chatMessageRepository.findTop1ByMatchIdOrderByCreatedAtDesc(match.getId());
            if (lastMessage == null) {
                // No messages yet in this match chat
                continue;
            }

            MatchChatReadState readState = matchChatReadStateRepository
                    .findByUserIdAndMatchId(currentUser.getId(), match.getId())
                    .orElse(null);
            OffsetDateTime lastReadAt = readState != null ? readState.getLastReadAt() : null;
            long unread = (lastReadAt == null)
                    ? chatMessageRepository
                            .countUnreadForUserInMatch(match.getId(), currentUser.getId())
                    : chatMessageRepository
                            .countUnreadForUserInMatchSince(match.getId(), currentUser.getId(), lastReadAt);

            String subtitle = match.getCommunity() != null ? match.getCommunity().getName() : null;

            result.add(ActiveChatSummaryResponse.builder()
                    .chatType(ChatType.MATCH_EVENT)
                    .targetId(match.getId())
                    .title(match.getTitle())
                    .subtitle(subtitle)
                    .imageUrl(match.getBannerUrl())
                    .lastMessage(lastMessage.getMessage())
                    .lastMessageAt(lastMessage.getCreatedAt())
                    .unreadCount(unread)
                    .build());
        }

        return result;
    }

    private String buildConversationId(UUID userA, UUID userB) {
        String a = userA.toString();
        String b = userB.toString();
        if (a.compareTo(b) < 0) {
            return a + ":" + b;
        }
        return b + ":" + a;
    }
}


