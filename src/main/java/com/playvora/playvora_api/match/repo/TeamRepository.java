package com.playvora.playvora_api.match.repo;

import com.playvora.playvora_api.match.entities.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    
    List<Team> findByMatchId(UUID matchId);
    
    @Query("SELECT DISTINCT t FROM Team t LEFT JOIN FETCH t.players p LEFT JOIN FETCH p.user LEFT JOIN FETCH t.captain WHERE t.match.id = :matchId")
    List<Team> findByMatchIdWithPlayers(@Param("matchId") UUID matchId);
    
    @Query("SELECT COUNT(t) FROM Team t WHERE t.match.id = :matchId")
    Long countTeamsByMatchId(@Param("matchId") UUID matchId);
    
    @Query("SELECT t FROM Team t JOIN t.players tp WHERE tp.user.id = :userId")
    List<Team> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT t FROM Team t WHERE t.captain.id = :captainId")
    List<Team> findByCaptainId(@Param("captainId") UUID captainId);

    Optional<Team> findByMatchIdAndName(UUID matchId, String name);
}
