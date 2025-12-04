package com.playvora.playvora_api.match.repo;

import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.match.enums.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    
    @Query("SELECT m FROM Match m WHERE m.community.id = :communityId")
    Page<Match> findByCommunityId(@Param("communityId") UUID communityId, Pageable pageable);
    
    @Query("SELECT m FROM Match m WHERE m.community.id = :communityId AND m.status = :status")
    Page<Match> findByCommunityIdAndStatus(@Param("communityId") UUID communityId, 
                                          @Param("status") MatchStatus status, 
                                          Pageable pageable);
    
    @Query("SELECT m FROM Match m WHERE m.matchDate >= :startDate AND m.matchDate <= :endDate")
    Page<Match> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                               @Param("endDate") LocalDateTime endDate, 
                               Pageable pageable);
    
    @Query("SELECT m FROM Match m WHERE m.community.id = :communityId AND m.matchDate >= :startDate AND m.matchDate <= :endDate")
    Page<Match> findByCommunityIdAndDateRange(@Param("communityId") UUID communityId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate,
                                             Pageable pageable);
    
    @Query("SELECT m FROM Match m WHERE m.registrationDeadline > :now AND m.status IN ('UPCOMING', 'REGISTRATION_OPEN')")
    List<Match> findOpenRegistrations(@Param("now") LocalDateTime now);
    
    @Query("SELECT m FROM Match m WHERE m.matchDate BETWEEN :startDate AND :endDate")
    List<Match> findUpcomingMatches(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT m FROM Match m JOIN m.availabilities a WHERE a.user.id = :userId")
    Page<Match> findByUserId(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT m FROM Match m JOIN m.availabilities a WHERE a.user.id = :userId AND a.status = :status")
    Page<Match> findByUserIdAndAvailabilityStatus(@Param("userId") UUID userId, 
                                                 @Param("status") String status, 
                                                 Pageable pageable);
    
    @Query("SELECT m FROM Match m WHERE m.id = :id")
    Optional<Match> findByIdWithDetails(@Param("id") UUID id);
    
    @Query("SELECT DISTINCT m FROM Match m LEFT JOIN FETCH m.community WHERE m.id = :id")
    Optional<Match> findByIdWithCommunity(@Param("id") UUID id);

    @Query("""
        SELECT m FROM Match m
        LEFT JOIN m.community c
        WHERE (:search IS NULL 
               OR LOWER(m.title) LIKE :search
               OR LOWER(m.description) LIKE :search
               OR LOWER(c.name) LIKE :search)
    """)
    Page<Match> searchMatches(@Param("search") String search, Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        LEFT JOIN m.community c
        WHERE m.community.id = :communityId
          AND (:search IS NULL
               OR LOWER(m.title) LIKE :search
               OR LOWER(m.description) LIKE :search
               OR LOWER(c.name) LIKE :search)
    """)
    Page<Match> searchMatchesByCommunity(@Param("communityId") UUID communityId,
                                         @Param("search") String search,
                                         Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        LEFT JOIN m.community c
        WHERE m.matchDate >= :startDate AND m.matchDate <= :endDate
          AND (:search IS NULL 
               OR LOWER(m.title) LIKE :search
               OR LOWER(m.description) LIKE :search
               OR LOWER(c.name) LIKE :search)
    """)
    Page<Match> searchUpcomingMatches(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      @Param("search") String search,
                                      Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        LEFT JOIN m.community c
        WHERE m.community.id = :communityId
          AND m.matchDate >= :startDate AND m.matchDate <= :endDate
          AND (:search IS NULL
               OR LOWER(m.title) LIKE :search
               OR LOWER(m.description) LIKE :search
               OR LOWER(c.name) LIKE :search)
    """)
    Page<Match> searchUpcomingMatchesByCommunity(@Param("communityId") UUID communityId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate,
                                                 @Param("search") String search,
                                                 Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        LEFT JOIN m.community c
        WHERE m.createdBy.id = :creatorId
          AND (:search IS NULL
               OR LOWER(m.title) LIKE :search
               OR LOWER(m.description) LIKE :search
               OR LOWER(c.name) LIKE :search)
    """)
    Page<Match> searchMatchesByCreator(@Param("creatorId") UUID creatorId,
                                       @Param("search") String search,
                                       Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        LEFT JOIN m.community c
        WHERE m.createdBy.id = :creatorId
          AND m.matchDate >= :startDate AND m.matchDate <= :endDate
          AND (:search IS NULL
               OR LOWER(m.title) LIKE :search
               OR LOWER(m.description) LIKE :search
               OR LOWER(c.name) LIKE :search)
    """)
    Page<Match> searchUpcomingMatchesByCreator(@Param("creatorId") UUID creatorId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               @Param("search") String search,
                                               Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        WHERE m.community.id = :communityId
          AND m.createdBy.id = :creatorId
    """)
    Page<Match> findByCommunityIdAndCreatorId(@Param("communityId") UUID communityId,
                                              @Param("creatorId") UUID creatorId,
                                              Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        LEFT JOIN m.community c
        JOIN m.availabilities a
        WHERE a.user.id = :userId
          AND (:search IS NULL
               OR LOWER(m.title) LIKE :search
               OR LOWER(m.description) LIKE :search
               OR LOWER(c.name) LIKE :search)
    """)
    Page<Match> searchUserMatches(@Param("userId") UUID userId,
                                  @Param("search") String search,
                                  Pageable pageable);

    /**
     * Check if a community has any active (upcoming or ongoing) matches.
     * Active here means any status other than COMPLETED or CANCELLED.
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
           "FROM Match m " +
           "WHERE m.community.id = :communityId " +
           "AND m.status IN :statuses")
    boolean hasActiveEventsForCommunity(
            @Param("communityId") UUID communityId,
            @Param("statuses") List<MatchStatus> statuses);

    @Query("""
        SELECT m FROM Match m
        WHERE m.community.id = :communityId
          AND m.createdBy.id = :creatorId
          AND m.matchDate >= :startDate AND m.matchDate <= :endDate
    """)
    Page<Match> searchUpcomingMatchesByCommunityIdAndCreatorId(@Param("communityId") UUID communityId,
                                                               @Param("creatorId") UUID creatorId,
                                                               @Param("startDate") LocalDateTime startDate,
                                                               @Param("endDate") LocalDateTime endDate, Pageable pageable);

    @Query("""
        SELECT m FROM Match m
        WHERE m.community.id = :communityId
          AND m.matchDate >= :startDate AND m.matchDate <= :endDate
    """)
    Page<Match> searchUpcomingMatchesByCommunityId(@Param("communityId") UUID communityId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate, Pageable pageable);
}
