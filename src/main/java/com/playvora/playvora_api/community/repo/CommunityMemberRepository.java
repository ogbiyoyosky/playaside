package com.playvora.playvora_api.community.repo;

import com.playvora.playvora_api.community.entities.CommunityMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, UUID> {
    
    Optional<CommunityMember> findByCommunityIdAndUserId(UUID communityId, UUID userId);
    
    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
           "FROM CommunityMember cm " +
           "WHERE cm.community.id = :communityId " +
           "AND cm.user.id = :userId " +
           "AND cm.isActive = true")
    boolean existsByCommunityIdAndUserIdAndIsActiveTrue(@Param("communityId") UUID communityId, @Param("userId") UUID userId);
    
    @Query("SELECT cm FROM CommunityMember cm WHERE cm.user.id = :userId AND cm.isActive = true")
    List<CommunityMember> findActiveMembershipsByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT cm FROM CommunityMember cm JOIN FETCH cm.user WHERE cm.community.id = :communityId AND cm.isActive = true")
    List<CommunityMember> findActiveMembersByCommunityId(@Param("communityId") UUID communityId);
    
    @Query("SELECT COUNT(cm) FROM CommunityMember cm WHERE cm.community.id = :communityId AND cm.isActive = true")
    Long countActiveMembersByCommunityId(@Param("communityId") UUID communityId);
}
