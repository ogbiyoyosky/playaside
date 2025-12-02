package com.playvora.playvora_api.notification.repo;

import com.playvora.playvora_api.notification.entities.DeviceToken;
import com.playvora.playvora_api.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    
    Optional<DeviceToken> findByUserAndToken(User user, String token);
    
    List<DeviceToken> findByUserAndIsActiveTrue(User user);
    
    List<DeviceToken> findByUserIdAndIsActiveTrue(UUID userId);
    
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.user.id IN :userIds AND dt.isActive = true")
    List<DeviceToken> findByUserIdsAndIsActiveTrue(@Param("userIds") List<UUID> userIds);
    
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.user = :user AND dt.token = :token")
    void deactivateToken(@Param("user") User user, @Param("token") String token);
    
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.token = :token")
    void deactivateTokenByToken(@Param("token") String token);
}

