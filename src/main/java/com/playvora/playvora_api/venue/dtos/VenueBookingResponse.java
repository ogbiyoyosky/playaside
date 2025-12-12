package com.playvora.playvora_api.venue.dtos;

import com.playvora.playvora_api.venue.enums.RentType;
import com.playvora.playvora_api.venue.enums.VenueBookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private RentType rentType;
    private BigDecimal totalPrice;
    private VenueBookingStatus status;
    private String paymentReference;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}


