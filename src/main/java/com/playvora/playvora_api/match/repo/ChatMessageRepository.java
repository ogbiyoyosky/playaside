package com.playvora.playvora_api.match.repo;

import com.playvora.playvora_api.match.entities.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}


