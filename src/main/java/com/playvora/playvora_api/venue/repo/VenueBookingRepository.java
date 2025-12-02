package com.playvora.playvora_api.venue.repo;

import com.playvora.playvora_api.venue.entities.VenueBooking;
import com.playvora.playvora_api.venue.enums.VenueBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VenueBookingRepository extends JpaRepository<VenueBooking, UUID> {

    Page<VenueBooking> findByUserId(UUID userId, Pageable pageable);

    @Query("""
            SELECT COUNT(b) > 0 FROM VenueBooking b
            WHERE b.venue.id = :venueId
              AND b.status = :status
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    boolean existsOverlappingBooking(
            @Param("venueId") UUID venueId,
            @Param("status") VenueBookingStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}


