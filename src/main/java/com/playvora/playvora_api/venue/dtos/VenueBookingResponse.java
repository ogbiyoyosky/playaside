package com.playvora.playvora_api.venue.dtos;

import com.playvora.playvora_api.venue.enums.RentType;
import com.playvora.playvora_api.venue.enums.VenueBookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VenueBookingResponse {

    private UUID id;
    private UUID venueId;
    private String venueName;
    private UUID userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RentType rentType;
    private BigDecimal totalPrice;
    private VenueBookingStatus status;
    private String paymentReference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


