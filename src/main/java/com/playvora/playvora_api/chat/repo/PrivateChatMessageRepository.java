package com.playvora.playvora_api.chat.repo;

import com.playvora.playvora_api.chat.entities.PrivateChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrivateChatMessageRepository extends JpaRepository<PrivateChatMessage, UUID> {

    /**
     * Fetch a conversation between two users, ordered by creation time.
     */
    @Query("SELECT m FROM PrivateChatMessage m " +
           "LEFT JOIN FETCH m.sender " +
           "LEFT JOIN FETCH m.recipient " +
           "WHERE (m.sender.id = :userA AND m.recipient.id = :userB) " +
           "   OR (m.sender.id = :userB AND m.recipient.id = :userA) " +
           "ORDER BY m.createdAt ASC")
    Page<PrivateChatMessage> findConversation(@Param("userA") UUID userA,
                                              @Param("userB") UUID userB,
                                              Pageable pageable);

    /**
     * Count unread messages for a conversation, from a specific other user to the recipient.
     * Variant without timestamp (no last-read state yet).
     */
    @Query("SELECT COUNT(m) FROM PrivateChatMessage m " +
           "WHERE m.recipient.id = :recipientId " +
           "AND m.sender.id = :otherUserId")
    long countUnreadForRecipientInConversation(@Param("recipientId") UUID recipientId,
                                               @Param("otherUserId") UUID otherUserId);

    /**
     * Count unread messages for a conversation since the given last-read timestamp.
     */
    @Query("SELECT COUNT(m) FROM PrivateChatMessage m " +
           "WHERE m.recipient.id = :recipientId " +
           "AND m.sender.id = :otherUserId " +
           "AND m.createdAt > :lastReadAt")
    long countUnreadForRecipientInConversationSince(@Param("recipientId") UUID recipientId,
                                                    @Param("otherUserId") UUID otherUserId,
                                                    @Param("lastReadAt") java.time.OffsetDateTime lastReadAt);

    /**
     * Fetch recent messages for all conversations a user participates in,
     * ordered by newest first. This is used to derive the latest message per
     * conversation for the \"active chats\" endpoint.
     */
    List<PrivateChatMessage> findTop500BySenderIdOrRecipientIdOrderByCreatedAtDesc(UUID senderId, UUID recipientId);
}


