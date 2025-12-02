package com.playvora.playvora_api.payment.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateSubscriptionRequest {
    // For user-based subscriptions, communityId is no longer required.
    // Payment method is optional when starting a trial.
    private String paymentMethodId;
    private String couponCode;
    @Builder.Default
    private boolean startTrial = true;
}
