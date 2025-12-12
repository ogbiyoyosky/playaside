package com.playvora.playvora_api.chat.repo;

import com.playvora.playvora_api.chat.entities.CommunityChatReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityChatReadStateRepository extends JpaRepository<CommunityChatReadState, UUID> {

    Optional<CommunityChatReadState> findByUserIdAndCommunityId(UUID userId, UUID communityId);
}


