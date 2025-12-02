package com.playvora.playvora_api.match.repo;

import com.playvora.playvora_api.match.entities.MatchRegistration;
import com.playvora.playvora_api.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRegistrationRepository extends JpaRepository<MatchRegistration, UUID> {

    boolean existsByMatchIdAndUserId(UUID matchId, UUID userId);

    /**
     * Find all match registrations for a given user.
     */
    List<MatchRegistration> findByUserId(UUID userId);

    /**
     * Find all users who have registered for a given match.
     */
    @Query("SELECT mr.user FROM MatchRegistration mr WHERE mr.match.id = :matchId")
    List<User> findUsersByMatchId(@Param("matchId") UUID matchId);
}


