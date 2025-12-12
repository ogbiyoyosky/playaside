package com.playvora.playvora_api.match.repo;

import com.playvora.playvora_api.match.entities.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    @Query("SELECT cm FROM ChatMessage cm " +
           "LEFT JOIN FETCH cm.sender " +
           "WHERE cm.match.id = :matchId " +
           "ORDER BY cm.createdAt DESC")
    Page<ChatMessage> findByMatchIdOrderByCreatedAtDesc(@Param("matchId") UUID matchId, Pageable pageable);
    
    @Query("SELECT cm FROM ChatMessage cm " +
           "LEFT JOIN FETCH cm.sender " +
           "WHERE cm.match.id = :matchId " +
           "ORDER BY cm.createdAt ASC")
    Page<ChatMessage> findByMatchIdOrderByCreatedAtAsc(@Param("matchId") UUID matchId, Pageable pageable);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
           "WHERE cm.match.id = :matchId " +
           "AND cm.sender.id <> :userId")
    long countUnreadForUserInMatch(@Param("matchId") UUID matchId,
                                   @Param("userId") UUID userId);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm " +
           "WHERE cm.match.id = :matchId " +
           "AND cm.sender.id <> :userId " +
           "AND cm.createdAt > :lastReadAt")
    long countUnreadForUserInMatchSince(@Param("matchId") UUID matchId,
                                        @Param("userId") UUID userId,
                                        @Param("lastReadAt") OffsetDateTime lastReadAt);

    /**
     * Fetch the latest message for a match event.
     */
    ChatMessage findTop1ByMatchIdOrderByCreatedAtDesc(UUID matchId);
}


