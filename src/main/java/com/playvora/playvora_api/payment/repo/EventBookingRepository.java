package com.playvora.playvora_api.payment.repo;

import com.playvora.playvora_api.payment.entities.EventBooking;
import com.playvora.playvora_api.payment.entities.Payment;
import com.playvora.playvora_api.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventBookingRepository extends JpaRepository<EventBooking, UUID> {

    Optional<EventBooking> findByReference(String reference);

    List<EventBooking> findByPayment(Payment payment);

    Page<EventBooking> findByUser(User user, Pageable pageable);

    Page<EventBooking> findByUserAndBookedAtBetween(User user, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    Page<EventBooking> findByUserAndBookedAtGreaterThanEqual(User user, OffsetDateTime from, Pageable pageable);

    Page<EventBooking> findByUserAndBookedAtLessThanEqual(User user, OffsetDateTime to, Pageable pageable);
}



