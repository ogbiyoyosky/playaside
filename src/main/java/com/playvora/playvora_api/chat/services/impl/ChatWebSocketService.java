package com.playvora.playvora_api.chat.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.chat.dtos.ChatNotification;
import com.playvora.playvora_api.chat.dtos.CommunityChatMessageRequest;
import com.playvora.playvora_api.chat.dtos.CommunityChatMessageResponse;
import com.playvora.playvora_api.chat.dtos.PrivateChatMessageRequest;
import com.playvora.playvora_api.chat.dtos.PrivateChatMessageResponse;
import com.playvora.playvora_api.chat.entities.CommunityChatMessage;
import com.playvora.playvora_api.chat.entities.CommunityChatReadState;
import com.playvora.playvora_api.chat.entities.PrivateChatMessage;
import com.playvora.playvora_api.chat.entities.PrivateChatReadState;
import com.playvora.playvora_api.chat.repo.CommunityChatMessageRepository;
import com.playvora.playvora_api.chat.repo.CommunityChatReadStateRepository;
import com.playvora.playvora_api.chat.repo.PrivateChatMessageRepository;
import com.playvora.playvora_api.chat.repo.PrivateChatReadStateRepository;
import com.playvora.playvora_api.chat.services.IChatWebSocketService;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.community.repo.CommunityMemberRepository;
import com.playvora.playvora_api.community.repo.CommunityRepository;
import com.playvora.playvora_api.match.dtos.websocket.WebSocketMessage;
import com.playvora.playvora_api.notification.services.IPushNotificationService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketService implements IChatWebSocketService {

    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final PrivateChatMessageRepository privateChatMessageRepository;
    private final CommunityChatMessageRepository communityChatMessageRepository;
    private final PrivateChatReadStateRepository privateChatReadStateRepository;
    private final CommunityChatReadStateRepository communityChatReadStateRepository;
    private final IPushNotificationService pushNotificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendPrivateChatMessage(UUID recipientId,
                                       PrivateChatMessageRequest request,
                                       SimpMessageHeaderAccessor headerAccessor) {
        try {
            User currentUser = getCurrentUser(headerAccessor);
            log.info("User {} sending private chat message to {}", currentUser.getId(), recipientId);

            if (recipientId == null) {
                throw new BadRequestException("Recipient ID is required");
            }

            // Optional payload consistency check
            if (request.getRecipientId() != null && !recipientId.equals(request.getRecipientId())) {
                throw new BadRequestException("Recipient ID mismatch between path and payload");
            }

            if (currentUser.getId().equals(recipientId)) {
                throw new BadRequestException("You cannot send a private message to yourself");
            }

            User recipient = userRepository.findById(recipientId)
                    .orElseThrow(() -> new BadRequestException("Recipient user not found"));

            // Persist message
            PrivateChatMessage message = PrivateChatMessage.builder()
                    .sender(currentUser)
                    .recipient(recipient)
                    .message(request.getMessage())
                    .build();
            message = privateChatMessageRepository.save(message);

            // Update read state for sender - they have read up to this message
            PrivateChatReadState senderReadState = privateChatReadStateRepository
                    .findByUserIdAndOtherUserId(currentUser.getId(), recipientId)
                    .orElse(PrivateChatReadState.builder()
                            .user(currentUser)
                            .otherUser(recipient)
                            .build());
            senderReadState.setLastReadAt(message.getCreatedAt());
            privateChatReadStateRepository.save(senderReadState);

            String senderName = currentUser.getFirstName() + " " + currentUser.getLastName();
            String recipientName = recipient.getFirstName() + " " + recipient.getLastName();

            // Deterministic conversation id: sorted user ids joined with ":"
            String conversationId = buildConversationId(currentUser.getId(), recipientId);

            PrivateChatMessageResponse response = PrivateChatMessageResponse.builder()
                    .id(message.getId())
                    .senderId(currentUser.getId())
                    .senderName(senderName)
                    .recipientId(recipientId)
                    .recipientName(recipientName)
                    .message(message.getMessage())
                    .createdAt(message.getCreatedAt())
                    .senderProfilePictureUrl(currentUser.getProfilePictureUrl())
                    .recipientProfilePictureUrl(recipient.getProfilePictureUrl())
                    .conversationId(conversationId)
                    .build();

            // Notify both sender and recipient over WebSocket user queues.
            // IMPORTANT: Spring's user destinations resolve based on Principal.getName(),
            // which in our case is the authenticated user's email (see AppUserDetail#getUsername()).
            // Therefore we must use email here, not the UUID, so that subscriptions to
            // "/user/queue/private-chat" receive these messages correctly.
            WebSocketMessage wsMessage = WebSocketMessage.create("private_chat_message", response);

            messagingTemplate.convertAndSendToUser(
                    currentUser.getEmail(),
                    "/queue/private-chat",
                    wsMessage
            );

            messagingTemplate.convertAndSendToUser(
                    recipient.getEmail(),
                    "/queue/private-chat",
                    wsMessage
            );

            // Send chat notifications to both users for unified chat updates
            sendChatNotification(ChatNotification.ChatType.PRIVATE, recipientId, message.getId(),
                               currentUser.getId(), senderName, request.getMessage(),
                               message.getCreatedAt(), currentUser.getEmail());

            sendChatNotification(ChatNotification.ChatType.PRIVATE, currentUser.getId(), message.getId(),
                               currentUser.getId(), senderName, request.getMessage(),
                               message.getCreatedAt(), recipient.getEmail());

            // Push notification to recipient
            try {
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("type", "PRIVATE_CHAT_MESSAGE");
                notificationData.put("senderId", currentUser.getId().toString());
                notificationData.put("senderName", senderName);
                notificationData.put("recipientId", recipientId.toString());
                notificationData.put("conversationId", conversationId);
                notificationData.put("messageId", message.getId().toString());

                String title = "New message from " + senderName;
                String body = request.getMessage();

                pushNotificationService.sendPushNotificationToUser(
                        recipientId.toString(),
                        title,
                        body,
                        notificationData
                );
            } catch (Exception e) {
                log.error("Error sending push notification for private chat: {}", e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Error sending private chat message: {}", e.getMessage(), e);

            try {
                User currentUser = getCurrentUser(headerAccessor);
                messagingTemplate.convertAndSendToUser(
                        currentUser.getEmail(),
                        "/queue/errors",
                        WebSocketMessage.create("error", "Failed to send private chat message: " + e.getMessage())
                );
            } catch (Exception ignored) {
                log.warn("Could not send error message to user: {}", ignored.getMessage());
            }
        }
    }

    @Override
    public void sendCommunityChatMessage(UUID communityId,
                                         CommunityChatMessageRequest request,
                                         SimpMessageHeaderAccessor headerAccessor) {
        try {
            User currentUser = getCurrentUser(headerAccessor);
            log.info("User {} sending community chat message in community {}", currentUser.getId(), communityId);

            if (!communityId.equals(request.getCommunityId())) {
                throw new BadRequestException("Community ID mismatch between path and payload");
            }

            Community community = communityRepository.findById(communityId)
                    .orElseThrow(() -> new BadRequestException("Community not found"));

            boolean isMember = communityMemberRepository
                    .existsByCommunityIdAndUserIdAndIsActiveTrue(communityId, currentUser.getId());
            if (!isMember) {
                throw new BadRequestException("You must be a member of this community to send messages");
            }

            CommunityChatMessage message = CommunityChatMessage.builder()
                    .community(community)
                    .sender(currentUser)
                    .message(request.getMessage())
                    .build();
            message = communityChatMessageRepository.save(message);

            // Update read state for sender - they have read up to this message
            CommunityChatReadState senderReadState = communityChatReadStateRepository
                    .findByUserIdAndCommunityId(currentUser.getId(), communityId)
                    .orElse(CommunityChatReadState.builder()
                            .user(currentUser)
                            .community(community)
                            .build());
            senderReadState.setLastReadAt(message.getCreatedAt());
            communityChatReadStateRepository.save(senderReadState);

            String senderName = currentUser.getFirstName() + " " + currentUser.getLastName();

            CommunityChatMessageResponse response = CommunityChatMessageResponse.builder()
                    .id(message.getId())
                    .communityId(communityId)
                    .senderId(currentUser.getId())
                    .senderName(senderName)
                    .message(message.getMessage())
                    .senderProfilePictureUrl(currentUser.getProfilePictureUrl())
                    .createdAt(message.getCreatedAt())
                    .build();

            String topic = "/topic/communities/" + communityId + "/chat";
            WebSocketMessage wsMessage = WebSocketMessage.create("community_chat_message", response);
            messagingTemplate.convertAndSend(topic, wsMessage);

            log.info("Community chat message broadcasted to topic: {}", topic);

            // Send chat notifications to all active community members except sender
            try {
                var members = communityMemberRepository.findActiveMembersByCommunityId(communityId);
                for (var member : members) {
                    if (!member.getUser().getId().equals(currentUser.getId())) {
                        sendChatNotification(ChatNotification.ChatType.COMMUNITY, communityId, message.getId(),
                                           currentUser.getId(), senderName, request.getMessage(),
                                           message.getCreatedAt(), member.getUser().getEmail());
                    }
                }
            } catch (Exception e) {
                log.error("Error sending chat notifications for community chat: {}", e.getMessage(), e);
            }

            // Push notifications to all active community members except sender
            try {
                var members = communityMemberRepository.findActiveMembersByCommunityId(communityId);
                List<String> memberUserIds = members.stream()
                        .map(m -> m.getUser().getId().toString())
                        .filter(id -> !id.equals(currentUser.getId().toString()))
                        .collect(Collectors.toList());

                if (!memberUserIds.isEmpty()) {
                    Map<String, Object> notificationData = new HashMap<>();
                    notificationData.put("type", "COMMUNITY_CHAT_MESSAGE");
                    notificationData.put("communityId", communityId.toString());
                    notificationData.put("senderId", currentUser.getId().toString());
                    notificationData.put("senderName", senderName);
                    notificationData.put("messageId", message.getId().toString());

                    String title = "New message in " + community.getName();
                    String body = senderName + ": " + request.getMessage();

                    pushNotificationService.sendPushNotificationToUsers(
                            memberUserIds,
                            title,
                            body,
                            notificationData
                    );
                }
            } catch (Exception e) {
                log.error("Error sending push notifications for community chat: {}", e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("Error sending community chat message: {}", e.getMessage(), e);

            try {
                User currentUser = getCurrentUser(headerAccessor);
                messagingTemplate.convertAndSendToUser(
                        currentUser.getEmail(),
                        "/queue/errors",
                        WebSocketMessage.create("error", "Failed to send community chat message: " + e.getMessage())
                );
            } catch (Exception ignored) {
                log.warn("Could not send error message to user: {}", ignored.getMessage());
            }
        }
    }

    /**
     * Sends a chat notification to a specific user via the unified chat notifications queue.
     * This provides real-time updates for all chat types regardless of whether the user is
     * currently viewing that specific chat.
     */
    private void sendChatNotification(ChatNotification.ChatType chatType,
                                      UUID targetId,
                                      UUID messageId,
                                      UUID senderId,
                                      String senderName,
                                      String messagePreview,
                                      OffsetDateTime timestamp,
                                      String recipientEmail) {
        try {
            // Create preview (first ~50 characters)
            String preview = messagePreview;
            if (preview != null && preview.length() > 50) {
                preview = preview.substring(0, 47) + "...";
            }

            ChatNotification notification = ChatNotification.builder()
                    .chatType(chatType)
                    .targetId(targetId)
                    .messageId(messageId)
                    .senderId(senderId)
                    .senderName(senderName)
                    .preview(preview)
                    .timestamp(timestamp)
                    .build();

            WebSocketMessage wsMessage = WebSocketMessage.create("chat_notification", notification);
            messagingTemplate.convertAndSendToUser(
                    recipientEmail,
                    "/queue/chat-notifications",
                    wsMessage
            );

            log.debug("Chat notification sent to user {} for {} chat {}: {}",
                     recipientEmail, chatType, targetId, messageId);
        } catch (Exception e) {
            log.error("Error sending chat notification to user {}: {}", recipientEmail, e.getMessage(), e);
        }
    }

    private String buildConversationId(UUID userA, UUID userB) {
        String a = userA.toString();
        String b = userB.toString();
        if (a.compareTo(b) < 0) {
            return a + ":" + b;
        }
        return b + ":" + a;
    }

    /**
     * Resolve the current authenticated {@link User} from WebSocket headers / session and SecurityContext.
     * This mirrors the logic in {@code MatchWebSocketService} so authentication works the same way.
     */
    private User getCurrentUser(SimpMessageHeaderAccessor headerAccessor) {
        log.info("ChatWebSocketService.getCurrentUser called - Session ID: {}",
                headerAccessor != null ? headerAccessor.getSessionId() : "null");

        org.springframework.security.core.Authentication authentication = null;

        if (headerAccessor != null) {
            java.security.Principal principal = headerAccessor.getUser();
            log.info("HeaderAccessor.getUser() returned: {}",
                    principal != null ? principal.getClass().getName() : "null");

            if (principal != null) {
                if (principal instanceof org.springframework.security.core.Authentication) {
                    authentication = (org.springframework.security.core.Authentication) principal;
                    log.info("✓ Direct cast successful - Authentication type: {}",
                            authentication.getClass().getName());
                } else {
                    try {
                        authentication = org.springframework.security.core.Authentication.class.cast(principal);
                        log.info("✓ Explicit cast successful - Authentication type: {}",
                                authentication.getClass().getName());
                    } catch (ClassCastException e) {
                        log.error("✗ Cannot cast principal to Authentication. Principal type: {}, Error: {}",
                                principal.getClass().getName(), e.getMessage());
                    }
                }

                if (authentication != null) {
                    try {
                        Object authPrincipal = authentication.getPrincipal();
                        if (authPrincipal != null) {
                            String userName = authentication.getName();
                            log.info("✓✓ Authentication is valid (user: {}, principal type: {})",
                                    userName, authPrincipal.getClass().getName());

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.info("✓ Authentication set in SecurityContext from HeaderAccessor");
                        } else {
                            log.error("✗✗ Authentication exists but principal is NULL!");
                            authentication = null;
                        }
                    } catch (Exception e) {
                        log.error("✗✗ Error accessing authentication principal: {}", e.getMessage(), e);
                        authentication = null;
                    }
                }
            }

            if (authentication == null || authentication.getPrincipal() == null) {
                Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
                log.info("Checking session attributes - Available keys: {}",
                        sessionAttributes != null ? sessionAttributes.keySet() : "null");

                if (sessionAttributes != null) {
                    Object authObj = sessionAttributes.get("authentication");
                    if (authObj instanceof org.springframework.security.core.Authentication) {
                        authentication = (org.springframework.security.core.Authentication) authObj;
                        log.info("✓ Authentication retrieved from session attributes (user: {})",
                                authentication.getName());

                        headerAccessor.setUser(authentication);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("✓ Authentication set in SecurityContext from session attributes");
                    }
                }
            }
        }

        if (authentication == null) {
            org.springframework.security.core.Authentication securityAuth =
                    SecurityContextHolder.getContext().getAuthentication();
            if (securityAuth != null) {
                authentication = securityAuth;
                log.info("✓ Authentication found in SecurityContext (user: {})", authentication.getName());
            } else {
                log.warn("✗ SecurityContext.getAuthentication() is null");
            }
        }

        if (authentication == null) {
            log.error("✗✗✗ No authentication found in any location! Session ID: {}",
                    headerAccessor != null ? headerAccessor.getSessionId() : "null");
            throw new BadRequestException("Authentication required. Please provide a valid access token.");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            log.error("✗✗✗ Authentication found but principal is NULL! Authentication type: {}",
                    authentication.getClass().getName());
            throw new BadRequestException("Invalid authentication: principal is null");
        }

        log.info("Authentication principal type: {}", principal.getClass().getName());

        if (principal instanceof AppUserDetail userDetail) {
            log.info("Principal is AppUserDetail, email: {}", userDetail.getUsername());
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }

        log.error("Invalid authentication principal type: {}. Expected AppUserDetail",
                principal.getClass().getName());
        throw new BadRequestException("Invalid authentication principal type: " + principal.getClass().getName());
    }
}


