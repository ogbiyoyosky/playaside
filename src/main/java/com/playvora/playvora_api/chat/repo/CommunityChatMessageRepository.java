package com.playvora.playvora_api.chat.repo;

import com.playvora.playvora_api.chat.entities.CommunityChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface CommunityChatMessageRepository extends JpaRepository<CommunityChatMessage, UUID> {

    @Query("SELECT m FROM CommunityChatMessage m " +
           "LEFT JOIN FETCH m.sender " +
           "WHERE m.community.id = :communityId " +
           "ORDER BY m.createdAt ASC")
    Page<CommunityChatMessage> findByCommunityIdOrderByCreatedAtAsc(@Param("communityId") UUID communityId,
                                                                    Pageable pageable);

    @Query("SELECT COUNT(m) FROM CommunityChatMessage m " +
           "WHERE m.community.id = :communityId " +
           "AND m.sender.id <> :userId")
    long countUnreadForUserInCommunity(@Param("communityId") UUID communityId,
                                       @Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM CommunityChatMessage m " +
           "WHERE m.community.id = :communityId " +
           "AND m.sender.id <> :userId " +
           "AND m.createdAt > :lastReadAt")
    long countUnreadForUserInCommunitySince(@Param("communityId") UUID communityId,
                                            @Param("userId") UUID userId,
                                            @Param("lastReadAt") OffsetDateTime lastReadAt);

    /**
     * Fetch the latest message for a community.
     */
    CommunityChatMessage findTop1ByCommunityIdOrderByCreatedAtDesc(UUID communityId);
}


