package com.playvora.playvora_api.payment.services;

import com.playvora.playvora_api.payment.dtos.PayoutSetupResponse;
import com.playvora.playvora_api.payment.entities.Transaction;

import java.util.UUID;

public interface IPayoutService {

    /**
     * Ensure the current community manager has a Stripe connected account and
     * create an Account Session for managing payouts.
     *
     * @return PayoutSetupResponse containing the Stripe account ID and
     *         Account Session client secret.
     */
    PayoutSetupResponse setupPayout();

    /**
     * Trigger a Stripe payout for a scheduled payout record and record
     * a corresponding transaction (with push notification).
     *
     * @param payoutId the ID of the payout to withdraw
     * @return the created Transaction entity
     */
    Transaction withdrawPayout(UUID payoutId);
}


