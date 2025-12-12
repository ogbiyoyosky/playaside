package com.playvora.playvora_api.payment.dtos;

import com.playvora.playvora_api.payment.enums.SubscriptionStatus;
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
public class SubscriptionResponse {
    private UUID id;
    private UUID communityId;
    private String communityName;
    private String stripeSubscriptionId;
    private SubscriptionStatus status;
    private BigDecimal amount;
    private String currency;
    private String billingCycle;
    private OffsetDateTime trialStartDate;
    private OffsetDateTime trialEndDate;
    private OffsetDateTime currentPeriodStart;
    private OffsetDateTime currentPeriodEnd;
    private OffsetDateTime nextBillingDate;
    private OffsetDateTime canceledAt;
    private OffsetDateTime createdAt;
    private boolean isInTrial;
    private boolean isActive;
}
