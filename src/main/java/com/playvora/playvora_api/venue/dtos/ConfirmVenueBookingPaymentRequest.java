package com.playvora.playvora_api.venue.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfirmVenueBookingPaymentRequest {

    @NotBlank(message = "Payment reference is required")
    private String paymentReference;
}


