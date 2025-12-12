package com.playvora.playvora_api.payment.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SavePaymentMethodRequest {

    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;

    @Builder.Default
    private boolean setAsDefault = false;
}