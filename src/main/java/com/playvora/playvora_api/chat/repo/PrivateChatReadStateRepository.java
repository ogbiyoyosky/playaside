package com.playvora.playvora_api.chat.repo;

import com.playvora.playvora_api.chat.entities.PrivateChatReadState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrivateChatReadStateRepository extends JpaRepository<PrivateChatReadState, UUID> {

    Optional<PrivateChatReadState> findByUserIdAndOtherUserId(UUID userId, UUID otherUserId);
}


