package com.playvora.playvora_api.match.repo;

import com.playvora.playvora_api.match.entities.TeamPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamPlayerRepository extends JpaRepository<TeamPlayer, UUID> {
    
    List<TeamPlayer> findByTeamId(UUID teamId);
    
    Optional<TeamPlayer> findByTeamIdAndUserId(UUID teamId, UUID userId);
    
    @Query("SELECT tp FROM TeamPlayer tp WHERE tp.team.match.id = :matchId")
    List<TeamPlayer> findByMatchId(@Param("matchId") UUID matchId);
    
    @Query("SELECT tp FROM TeamPlayer tp WHERE tp.team.match.id = :matchId AND tp.user.id = :userId")
    Optional<TeamPlayer> findByMatchIdAndUserId(@Param("matchId") UUID matchId, @Param("userId") UUID userId);
    
    @Query("SELECT COUNT(tp) FROM TeamPlayer tp WHERE tp.team.id = :teamId")
    Long countPlayersByTeamId(@Param("teamId") UUID teamId);
    
    @Query("SELECT COUNT(tp) FROM TeamPlayer tp WHERE tp.team.match.id = :matchId")
    Long countPlayersByMatchId(@Param("matchId") UUID matchId);
    
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);
    
    @Query("SELECT tp FROM TeamPlayer tp WHERE tp.user.id = :userId")
    List<TeamPlayer> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT tp FROM TeamPlayer tp WHERE tp.isCaptain = true AND tp.team.id = :teamId")
    Optional<TeamPlayer> findCaptainByTeamId(@Param("teamId") UUID teamId);
}
