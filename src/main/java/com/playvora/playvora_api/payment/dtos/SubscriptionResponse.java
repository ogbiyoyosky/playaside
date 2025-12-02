package com.playvora.playvora_api.payment.dtos;

import com.playvora.playvora_api.payment.enums.SubscriptionStatus;
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
public class SubscriptionResponse {
    private UUID id;
    private UUID communityId;
    private String communityName;
    private String stripeSubscriptionId;
    private SubscriptionStatus status;
    private BigDecimal amount;
    private String currency;
    private String billingCycle;
    private LocalDateTime trialStartDate;
    private LocalDateTime trialEndDate;
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime nextBillingDate;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private boolean isInTrial;
    private boolean isActive;
}
