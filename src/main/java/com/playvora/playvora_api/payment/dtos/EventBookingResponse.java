package com.playvora.playvora_api.payment.dtos;

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
public class EventBookingResponse {

    private UUID id;
    private UUID paymentId;
    private UUID matchId;
    private UUID userId;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private OffsetDateTime bookedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}


