package com.playvora.playvora_api.match.repo;

import com.playvora.playvora_api.match.entities.MatchChatReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchChatReadStateRepository extends JpaRepository<MatchChatReadState, UUID> {

    Optional<MatchChatReadState> findByUserIdAndMatchId(UUID userId, UUID matchId);
}


