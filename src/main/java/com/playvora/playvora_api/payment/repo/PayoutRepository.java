package com.playvora.playvora_api.payment.repo;

import com.playvora.playvora_api.payment.entities.Payout;
import com.playvora.playvora_api.payment.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    List<Payout> findByStatus(PayoutStatus status);

    @Query("SELECT p FROM Payout p " +
           "WHERE p.match.id = :matchId")
    List<Payout> findByMatchId(@Param("matchId") UUID matchId);

    @Query("SELECT p FROM Payout p " +
           "WHERE p.status = :status " +
           "AND p.scheduledPayoutDate <= :maxScheduledDate")
    List<Payout> findDuePayouts(@Param("status") PayoutStatus status,
                                @Param("maxScheduledDate") OffsetDateTime maxScheduledDate);
}


