package com.playvora.playvora_api.user.repo;

import com.playvora.playvora_api.user.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    
    /**
     * Find all active user roles for a specific user
     */
    @Query("SELECT ur FROM UserRole ur " +
           "LEFT JOIN FETCH ur.role " +
           "LEFT JOIN FETCH ur.community " +
           "WHERE ur.user.id = :userId AND ur.deletedAt IS NULL")
    List<UserRole> findActiveUserRolesByUserId(@Param("userId") UUID userId);
    
    /**
     * Find all active user roles for a specific user in a specific community
     */
    @Query("SELECT ur FROM UserRole ur " +
           "LEFT JOIN FETCH ur.role " +
           "LEFT JOIN FETCH ur.community " +
           "WHERE ur.user.id = :userId " +
           "AND ur.community.id = :communityId " +
           "AND ur.deletedAt IS NULL")
    List<UserRole> findActiveUserRolesByUserIdAndCommunityId(
            @Param("userId") UUID userId, 
            @Param("communityId") UUID communityId);
    
    /**
     * Find a specific user role by ID (for header validation)
     */
    @Query("SELECT ur FROM UserRole ur " +
           "LEFT JOIN FETCH ur.role " +
           "LEFT JOIN FETCH ur.community " +
           "WHERE ur.id = :userRoleId AND ur.deletedAt IS NULL")
    Optional<UserRole> findActiveUserRoleById(@Param("userRoleId") UUID userRoleId);
    
    /**
     * Find a specific user role by user, role, and community
     */
    @Query("SELECT ur FROM UserRole ur " +
           "WHERE ur.user.id = :userId " +
           "AND ur.role.id = :roleId " +
           "AND (:communityId IS NULL AND ur.community IS NULL OR ur.community.id = :communityId) " +
           "AND ur.deletedAt IS NULL")
    Optional<UserRole> findActiveUserRole(
            @Param("userId") UUID userId, 
            @Param("roleId") UUID roleId, 
            @Param("communityId") UUID communityId);
    
    /**
     * Find global roles (not tied to any community) for a user
     */
    @Query("SELECT ur FROM UserRole ur " +
           "LEFT JOIN FETCH ur.role " +
           "WHERE ur.user.id = :userId " +
           "AND ur.community IS NULL " +
           "AND ur.deletedAt IS NULL")
    List<UserRole> findActiveGlobalRolesByUserId(@Param("userId") UUID userId);
    
    /**
     * Check if a user has a specific role in a community
     */
    @Query("SELECT CASE WHEN COUNT(ur) > 0 THEN true ELSE false END " +
           "FROM UserRole ur " +
           "WHERE ur.user.id = :userId " +
           "AND ur.role.name = :roleName " +
           "AND (:communityId IS NULL AND ur.community IS NULL OR ur.community.id = :communityId) " +
           "AND ur.deletedAt IS NULL")
    boolean hasRole(
            @Param("userId") UUID userId, 
            @Param("roleName") String roleName, 
            @Param("communityId") UUID communityId);

    /**
     * Soft delete all active roles for a given user in a specific community.
     */
    @Modifying
    @Query("UPDATE UserRole ur SET ur.deletedAt = CURRENT_TIMESTAMP " +
           "WHERE ur.user.id = :userId " +
           "AND ur.community.id = :communityId " +
           "AND ur.deletedAt IS NULL")
    int softDeleteByUserIdAndCommunityId(
            @Param("userId") UUID userId,
            @Param("communityId") UUID communityId);
}
