package com.playvora.playvora_api.payment.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.config.StripeConfig;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.exception.ForbiddenException;
import com.playvora.playvora_api.common.utils.UserRoleContext;
import com.playvora.playvora_api.payment.dtos.PayoutSetupResponse;
import com.playvora.playvora_api.payment.entities.Payout;
import com.playvora.playvora_api.payment.entities.Transaction;
import com.playvora.playvora_api.payment.enums.PayoutStatus;
import com.playvora.playvora_api.payment.enums.TransactionType;
import com.playvora.playvora_api.payment.repo.PayoutRepository;
import com.playvora.playvora_api.payment.services.IPayoutService;
import com.playvora.playvora_api.payment.services.ITransactionService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.entities.UserRole;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountSession;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService implements IPayoutService {

    private final UserRepository userRepository;
    private final StripeConfig stripeConfig;
    private final PayoutRepository payoutRepository;
    private final ITransactionService transactionService;

    @Override
    @Transactional
    public PayoutSetupResponse setupPayout() {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payout setup is currently unavailable");
        }

        User currentUser = getCurrentUser();
        validateManagerRole();

        boolean alreadySetup = currentUser.getConnectedAccountId() != null;
        String accountId = currentUser.getConnectedAccountId();

        try {
            // If the user does not yet have a connected account, create one now.
            if (!alreadySetup) {
                accountId = createConnectedAccount(currentUser);
                updateConnectedAccount(accountId);
                currentUser.setConnectedAccountId(accountId);
                userRepository.save(currentUser);
            }

            // Always create a fresh Account Session so the frontend can open
            // the Stripe embedded payout experience.
            String clientSecret = createAccountSession(accountId);

            return PayoutSetupResponse.builder()
                    .accountId(accountId)
                    .clientSecret(clientSecret)
                    .alreadySetup(alreadySetup)
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error during payout setup for user {}", currentUser.getId(), e);
            throw new BadRequestException("Failed to set up payouts with Stripe");
        } catch (Exception e) {
            log.error("Unexpected error during payout setup for user {}: {}", currentUser.getId(), e.getMessage(), e);
            throw new BadRequestException("Failed to set up payouts: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Transaction withdrawPayout(UUID payoutId) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payout processing is currently unavailable");
        }

        User currentUser = getCurrentUser();
        validateManagerRole();

        if (!currentUser.isPayoutsEnabled()) {
            throw new BadRequestException("Payout account is not fully enabled. Please complete Stripe verification.");
        }

        Payout payout = payoutRepository.findById(payoutId)
                .orElseThrow(() -> new BadRequestException("Payout not found"));

        if (!payout.getManagerUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not allowed to withdraw this payout");
        }

        if (payout.getStatus() == PayoutStatus.COMPLETED) {
            throw new BadRequestException("Payout has already been completed");
        }

        if (payout.getStatus() != PayoutStatus.SCHEDULED && payout.getStatus() != PayoutStatus.PENDING) {
            throw new BadRequestException("Payout is not in a withdrawable state");
        }

        if (payout.getScheduledPayoutDate() != null &&
                payout.getScheduledPayoutDate().isAfter(OffsetDateTime.now())) {
            throw new BadRequestException("Payout is not yet due");
        }

        if (currentUser.getConnectedAccountId() == null || currentUser.getConnectedAccountId().isBlank()) {
            throw new BadRequestException("Payout account is not configured. Please complete payout setup first.");
        }

        try {
            // Mark as processing before calling Stripe to avoid duplicate attempts.
            payout.setStatus(PayoutStatus.PROCESSING);
            payout = payoutRepository.save(payout);

            long amountInMinorUnit = payout.getTotalAmount()
                    .movePointRight(2)
                    .longValueExact();

            Map<String, Object> params = new HashMap<>();
            params.put("amount", amountInMinorUnit);
            params.put("currency", payout.getCurrency().toLowerCase());
            params.put("description", "Payout for match " + payout.getMatch().getId());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("payout_id", payout.getId().toString());
            if (payout.getMatch() != null) {
                metadata.put("match_id", payout.getMatch().getId().toString());
            }
            params.put("metadata", metadata);

            RequestOptions requestOptions = RequestOptions.builder()
                    .setStripeAccount(currentUser.getConnectedAccountId())
                    .build();

            com.stripe.model.Payout stripePayout = com.stripe.model.Payout.create(params, requestOptions);

            payout.setStatus(PayoutStatus.COMPLETED);
            payout.setProcessedAt(OffsetDateTime.now());
            payout.setFailureReason(null);
            payout = payoutRepository.save(payout);

            String description = "Payout for match " +
                    (payout.getMatch() != null ? payout.getMatch().getId() : payout.getId());

            return transactionService.recordGeneric(
                    currentUser,
                    null,
                    null,
                    payout,
                    payout.getMatch(),
                    TransactionType.PAYOUT,
                    payout.getTotalAmount(),
                    payout.getCurrency(),
                    description,
                    stripePayout.getId()
            );
        } catch (StripeException e) {
            payout.setStatus(PayoutStatus.FAILED);
            payout.setFailureReason(e.getMessage());
            payoutRepository.save(payout);

            log.error("Stripe error during payout withdrawal for payout {}: {}", payoutId, e.getMessage(), e);
            throw new BadRequestException("Failed to process payout with Stripe");
        } catch (Exception e) {
            payout.setStatus(PayoutStatus.FAILED);
            payout.setFailureReason(e.getMessage());
            payoutRepository.save(payout);

            log.error("Unexpected error during payout withdrawal for payout {}: {}", payoutId, e.getMessage(), e);
            throw new BadRequestException("Failed to process payout: " + e.getMessage());
        }
    }

    private void validateManagerRole() {
        UserRole currentUserRole = UserRoleContext.getCurrentUserRole();
        if (currentUserRole == null || currentUserRole.getRole() == null) {
            throw new ForbiddenException("Payout setup is only available when acting as a community manager");
        }

        String roleName = currentUserRole.getRole().getName();
        if (!"COMMUNITY_MANAGER".equals(roleName)) {
            throw new ForbiddenException("Payout setup is only available for COMMUNITY_MANAGER role");
        }
    }

    private String createConnectedAccount(User user) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "express");
        params.put("business_type", "individual");
        params.put("email", user.getEmail());

        // Use the user's country if available, otherwise default to GB (United Kingdom)
        String country = user.getCountry() != null ? user.getCountry() : "GB";
        params.put("country", country);

        Map<String, Object> capabilities = new HashMap<>();
        Map<String, Object> transfers = new HashMap<>();
        transfers.put("requested", true);
        capabilities.put("transfers", transfers);

        Map<String, Object> cardPayments = new HashMap<>();
        cardPayments.put("requested", true);
        capabilities.put("card_payments", cardPayments);

        params.put("capabilities", capabilities);

        Account account = Account.create(params);
        log.info("Created Stripe connected account {} for user {}", account.getId(), user.getId());
        return account.getId();
    }

    private String createAccountSession(String accountId) throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("account", accountId);

        Map<String, Object> components = new HashMap<>();
        Map<String, Object> onboardingComponent = new HashMap<>();
        onboardingComponent.put("enabled", true);

        Map<String, Object> features = new HashMap<>();
        features.put("document_uploads", true);
        onboardingComponent.put("features", features);

        components.put("account_onboarding", onboardingComponent);

        params.put("components", components);

        AccountSession session = AccountSession.create(params);
        log.info("Created Stripe AccountSession for account {}", accountId);
        return session.getClientSecret();
    }

    private void updateConnectedAccount(String accountId) throws StripeException {
        Account account = Account.retrieve(accountId);

        // Configure payouts schedule to be manual:
        // payouts: { schedule: { interval: "manual" } }
        Map<String, Object> schedule = new HashMap<>();
        schedule.put("interval", "manual");

        Map<String, Object> payouts = new HashMap<>();
        payouts.put("schedule", schedule);

        Map<String, Object> settings = new HashMap<>();
        settings.put("payouts", payouts);

        Map<String, Object> params = new HashMap<>();
        params.put("settings", settings);

        account = account.update(params);
        log.info("Updated Stripe connected account {} with manual payout schedule", account.getId());
    }
    

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }

        throw new BadRequestException("Invalid authentication principal");
    }
}


