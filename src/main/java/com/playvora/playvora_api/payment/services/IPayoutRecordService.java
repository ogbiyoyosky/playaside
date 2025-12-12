package com.playvora.playvora_api.payment.services;

public interface IPayoutRecordService {

    /**
     * Scan paid events and create payout records for matches whose payout date is due.
     *
     * This should:
     * - Find paid matches/events that do not yet have a payout.
     * - Compute the total amount collected for each event.
     * - Create a payout record scheduled for match_date + 1 day.
     */
    void createScheduledPayoutsForCompletedEvents();
}


