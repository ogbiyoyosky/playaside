package com.playvora.playvora_api.auth.repo;

import com.playvora.playvora_api.auth.entities.RefreshToken;
import com.playvora.playvora_api.user.entities.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findByUserAndRevokedFalse(User user);
    void deleteByUser(User user);
    void deleteByExpiresAtBefore(OffsetDateTime cutoff);
    void deleteByRevokedTrue();
}

