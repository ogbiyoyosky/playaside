package com.playvora.playvora_api.chat.dtos;

import com.playvora.playvora_api.chat.enums.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Aggregated view of an active chat for the current user.
 * Includes private, community and match/event chats.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveChatSummaryResponse {

    /**
     * Type of chat: PRIVATE, COMMUNITY, MATCH_EVENT.
     */
    private ChatType chatType;

    /**
     * Identifier of the chat target:
     * - PRIVATE: other user's id
     * - COMMUNITY: community id
     * - MATCH_EVENT: match id
     */
    private UUID targetId;

    /**
     * Human readable title:
     * - PRIVATE: other user's full name
     * - COMMUNITY: community name
     * - MATCH_EVENT: match title
     */
    private String title;

    /**
     * Optional secondary label (e.g. community name for match events).
     */
    private String subtitle;

    /**
     * Image URL for this chat:
     * - PRIVATE: other user's profile picture
     * - COMMUNITY: community banner URL
     * - MATCH_EVENT: match banner URL
     */
    private String imageUrl;

    /**
     * Content of the latest message in this chat.
     */
    private String lastMessage;

    /**
     * When the last message was created.
     */
    private OffsetDateTime lastMessageAt;

    /**
     * Unread message count for the current user in this chat.
     */
    private Long unreadCount;
}


