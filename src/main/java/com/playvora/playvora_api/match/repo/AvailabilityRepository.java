package com.playvora.playvora_api.match.repo;

import com.playvora.playvora_api.match.entities.Availability;
import com.playvora.playvora_api.match.enums.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {
    
    Optional<Availability> findByMatchIdAndUserId(UUID matchId, UUID userId);
    
    List<Availability> findByMatchId(UUID matchId);
    
    @Query("SELECT DISTINCT a FROM Availability a LEFT JOIN FETCH a.user WHERE a.match.id = :matchId")
    List<Availability> findByMatchIdWithUser(@Param("matchId") UUID matchId);
    
    List<Availability> findByMatchIdAndStatus(UUID matchId, AvailabilityStatus status);
    
    @Query("SELECT DISTINCT a FROM Availability a LEFT JOIN FETCH a.user WHERE a.match.id = :matchId AND a.status = 'AVAILABLE'")
    List<Availability> findAvailablePlayers(@Param("matchId") UUID matchId);
    
    @Query("SELECT COUNT(a) FROM Availability a WHERE a.match.id = :matchId AND a.status = 'AVAILABLE'")
    Long countAvailablePlayers(@Param("matchId") UUID matchId);
    
    @Query("SELECT COUNT(a) FROM Availability a WHERE a.match.id = :matchId")
    Long countTotalPlayers(@Param("matchId") UUID matchId);
    
    boolean existsByMatchIdAndUserId(UUID matchId, UUID userId);
    
    @Query("SELECT a FROM Availability a WHERE a.user.id = :userId")
    List<Availability> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT a FROM Availability a WHERE a.user.id = :userId AND a.status = :status")
    List<Availability> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") AvailabilityStatus status);
}
