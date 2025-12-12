package com.playvora.playvora_api.payment.services.impl;

import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.match.repo.MatchRepository;
import com.playvora.playvora_api.payment.entities.Payout;
import com.playvora.playvora_api.payment.entities.Payment;
import com.playvora.playvora_api.payment.enums.PaymentStatus;
import com.playvora.playvora_api.payment.enums.PaymentType;
import com.playvora.playvora_api.payment.enums.PayoutStatus;
import com.playvora.playvora_api.payment.repo.PayoutRepository;
import com.playvora.playvora_api.payment.repo.PaymentRepository;
import com.playvora.playvora_api.payment.services.IPayoutRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutRecordService implements IPayoutRecordService {

    private final MatchRepository matchRepository;
    private final PaymentRepository paymentRepository;
    private final PayoutRepository payoutRepository;

    /**
     * Public method (for manual triggering/tests) that delegates to the scheduled job logic.
     */
    @Override
    @Transactional
    public void createScheduledPayoutsForCompletedEvents() {
        processPayoutsForCompletedMatches();
    }

    /**
     * Daily job that runs shortly after midnight (server time) to create payout
     * records for completed, paid events whose payout date has just become due.
     *
     * cron = "0 10 0 * * *" → 00:10 every day
     */
    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void processPayoutsForCompletedMatches() {
        LocalDate today = LocalDate.now();

        // We want to create payouts for matches that took place "yesterday"
        // where the payout date is match_date + 1 day = today.
        OffsetDateTime startOfToday = today.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfToday = today.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);

        log.info("Running scheduled payout job for payout date between {} and {}", startOfToday, endOfToday);

        // Find completed, paid matches whose matchDate was yesterday and which are paid events.
        List<Match> completedMatches = matchRepository.findCompletedPaidMatchesWithinDateRange(
                startOfToday.minusDays(1),
                endOfToday.minusDays(1)
        );

        for (Match match : completedMatches) {
            try {
                // Skip if a payout already exists for this match
                if (!payoutRepository.findByMatchId(match.getId()).isEmpty()) {
                    log.debug("Payout already exists for match {}, skipping", match.getId());
                    continue;
                }

                // Sum all successful MATCH_FEE payments linked to this match
                List<Payment> payments = paymentRepository.findByMatchAndStatusAndType(
                        match.getId(),
                        PaymentStatus.SUCCEEDED,
                        PaymentType.MATCH_FEE
                );

                BigDecimal totalAmount = payments.stream()
                        .map(Payment::getAmount)
                        .filter(a -> a != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("No successful MATCH_FEE payments for match {}, skipping payout creation", match.getId());
                    continue;
                }

                Payout payout = Payout.builder()
                        .match(match)
                        .community(match.getCommunity())
                        .managerUser(match.getCreatedBy())
                        .totalAmount(totalAmount)
                        .currency(match.getCurrency())
                        .status(PayoutStatus.SCHEDULED)
                        .scheduledPayoutDate(match.getMatchDate().plusDays(1))
                        .build();

                payoutRepository.save(payout);
                log.info("Created payout record for match {} with amount {}", match.getId(), totalAmount);
            } catch (Exception ex) {
                log.error("Failed to create payout for match {}: {}", match.getId(), ex.getMessage(), ex);
            }
        }
    }
}


