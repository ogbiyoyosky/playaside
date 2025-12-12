package com.playvora.playvora_api.payment.repo;

import com.playvora.playvora_api.payment.entities.Subscription;
import com.playvora.playvora_api.payment.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
    
    Optional<Subscription> findByCommunityIdAndUserId(UUID communityId, UUID userId);
    
    List<Subscription> findByCommunityId(UUID communityId);
    
    List<Subscription> findByUserId(UUID userId);
    
    List<Subscription> findByStatus(SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.nextBillingDate <= :date")
    List<Subscription> findDueForBilling(@Param("status") SubscriptionStatus status, @Param("date") OffsetDateTime date);
    
    @Query("SELECT s FROM Subscription s WHERE s.community.id = :communityId AND s.status IN ('ACTIVE', 'TRIALING')")
    Optional<Subscription> findActiveSubscriptionByCommunityId(@Param("communityId") UUID communityId);
    
    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.community.id = :communityId AND s.status IN ('ACTIVE', 'TRIALING')")
    Long countActiveSubscriptionsByCommunityId(@Param("communityId") UUID communityId);
    
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'TRIALING')")
    List<Subscription> findActiveSubscriptionsByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'TRIALING')")
    Optional<Subscription> findActiveSubscriptionByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.user.id = :userId AND s.status IN ('ACTIVE', 'TRIALING')")
    Long countActiveSubscriptionsByUserId(@Param("userId") UUID userId);
}
