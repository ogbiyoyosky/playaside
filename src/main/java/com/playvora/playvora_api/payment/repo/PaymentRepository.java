package com.playvora.playvora_api.payment.repo;

import com.playvora.playvora_api.payment.entities.Payment;
import com.playvora.playvora_api.payment.enums.PaymentStatus;
import com.playvora.playvora_api.payment.enums.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Payment> findByStripeChargeId(String stripeChargeId);

    List<Payment> findByUserId(UUID userId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByType(PaymentType type);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId")
    Page<Payment> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByDateRange(@Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.user.id = :userId AND p.status = 'SUCCEEDED' AND p.type = :type")
    Double sumAmountByUserIdAndType(@Param("userId") UUID userId, @Param("type") PaymentType type);

    /**
     * Find all payments for a specific match with a given status and type,
     * by joining through event_bookings table.
     */
    @Query("SELECT DISTINCT p FROM Payment p " +
           "JOIN EventBooking eb ON eb.payment.id = p.id " +
           "WHERE eb.match.id = :matchId AND p.status = :status AND p.type = :type")
    List<Payment> findByMatchAndStatusAndType(@Param("matchId") UUID matchId,
                                              @Param("status") PaymentStatus status,
                                              @Param("type") PaymentType type);
}
