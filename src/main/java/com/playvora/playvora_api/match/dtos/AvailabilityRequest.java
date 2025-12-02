package com.playvora.playvora_api.match.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AvailabilityRequest {
    @NotNull(message = "Is available is required")
    private Boolean isAvailable;

    private BigDecimal userLatitude;
    private BigDecimal userLongitude;
}
