package com.playvora.playvora_api.payment.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayoutSetupResponse {
    /**
     * The client secret for the Stripe Account Session, used on the frontend
     * to initialize the embedded payout onboarding experience.
     */
    private String clientSecret;

    /**
     * The Stripe connected account ID (e.g. acct_xxx) associated with the
     * current manager user.
     */
    private String accountId;

    /**
     * Indicates whether the connected account already existed before this call.
     * If false, the account was created as part of this setup operation.
     */
    private boolean alreadySetup;
}


