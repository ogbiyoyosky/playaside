package com.playvora.playvora_api.payment.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.config.StripeConfig;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.payment.dtos.CreateSubscriptionRequest;
import com.playvora.playvora_api.payment.dtos.PaymentIntentRequest;
import com.playvora.playvora_api.payment.dtos.PaymentResponse;
import com.playvora.playvora_api.payment.dtos.SubscriptionResponse;
import com.playvora.playvora_api.payment.dtos.WalletCheckoutSessionRequest;
import com.playvora.playvora_api.payment.dtos.WalletCheckoutSessionResponse;
import com.playvora.playvora_api.payment.entities.Payment;
import com.playvora.playvora_api.payment.entities.Subscription;
import com.playvora.playvora_api.payment.enums.PaymentStatus;
import com.playvora.playvora_api.payment.enums.PaymentType;
import com.playvora.playvora_api.payment.enums.SubscriptionStatus;
import com.playvora.playvora_api.payment.repo.PaymentRepository;
import com.playvora.playvora_api.payment.repo.SubscriptionRepository;
import com.playvora.playvora_api.payment.services.IPaymentService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.wallet.entities.Wallet;
import com.playvora.playvora_api.wallet.services.IWalletService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements IPaymentService {
    
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final StripeConfig stripeConfig;
    private final IWalletService walletService;

    @Override
    @Transactional
    public PaymentResponse createPaymentIntent(PaymentIntentRequest request) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }
        
        User currentUser = getCurrentUser();

        try {
            // Determine currency: for wallet top-ups, always use the user's wallet currency
            String currency = "GBP";
            if (request.getType() == PaymentType.WALLET_DEPOSIT) {
                // Ensure the user has a wallet with a fixed currency
                Wallet wallet = walletService.getWalletForUser(currentUser);
                if (wallet == null) {
                    // Create wallet if it doesn't exist yet (will validate country)
                    wallet = walletService.createWalletForUser(currentUser);
                }
                currency = wallet.getCurrency();
            }

            // Stripe expects the amount in the smallest currency unit (e.g. pence)
            long amountInMinorUnit = request.getAmount()
                    .movePointRight(2)
                    .longValueExact();

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInMinorUnit)
                    .setCurrency(currency.toLowerCase())
                    .setDescription(request.getDescription())
                    .putMetadata("user_id", currentUser.getId().toString())
                    .putMetadata("payment_type", request.getType().name());

            PaymentIntentCreateParams params = paramsBuilder.build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Save payment record linked to the Stripe PaymentIntent
            Payment payment = Payment.builder()
                    .user(currentUser)
                    .stripePaymentIntentId(paymentIntent.getId())
                    .type(request.getType())
                    .status(PaymentStatus.PENDING)
                    .amount(request.getAmount())
                    .currency(currency)
                    .description(request.getDescription())
                    .build();

            payment = paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .id(payment.getId())
                    .type(payment.getType())
                    .status(payment.getStatus())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .description(payment.getDescription())
                    .clientSecret(paymentIntent.getClientSecret())
                    .paymentIntentId(paymentIntent.getId())
                    .createdAt(payment.getCreatedAt())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error creating payment intent", e);
            throw new BadRequestException("Failed to create payment intent with Stripe");
        } catch (Exception e) {
            log.error("Error creating payment intent: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create payment intent: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse confirmPayment(String paymentIntentId) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }
        
        try {
            // Retrieve the latest state of the PaymentIntent from Stripe
            PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);

            if (!"succeeded".equalsIgnoreCase(stripePaymentIntent.getStatus())) {
                throw new BadRequestException("Payment is not completed on Stripe");
            }

            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                    .orElseThrow(() -> new BadRequestException("Payment not found"));

            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setProcessedAt(LocalDateTime.now());
            payment.setPaymentMethod(stripePaymentIntent.getPaymentMethod());

            payment = paymentRepository.save(payment);

            // If this payment represents a wallet top-up, apply it to the user's wallet
            if (payment.getType() == PaymentType.WALLET_DEPOSIT) {
                walletService.topUpWallet(payment.getUser(), payment.getAmount());
            }

            return convertToPaymentResponse(payment);

        } catch (StripeException e) {
            log.error("Stripe error confirming payment intent {}", paymentIntentId, e);
            throw new BadRequestException("Failed to confirm payment with Stripe");
        } catch (Exception e) {
            log.error("Error confirming payment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to confirm payment: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(String paymentIntentId) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }
        
        throw new BadRequestException("Payment cancellation not implemented in mock mode");
    }

    @Override
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }
        // Real subscription creation with Stripe would go here (user-based).
        throw new BadRequestException("Subscription creation not implemented in mock mode");
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(String subscriptionId) {
        throw new BadRequestException("Subscription cancellation not implemented in mock mode");
    }

    @Override
    @Transactional
    public SubscriptionResponse updateSubscription(String subscriptionId, String paymentMethodId) {
        throw new BadRequestException("Subscription update not implemented in mock mode");
    }

    @Override
    public PaginatedResponse<PaymentResponse> getUserPayments(int page, int size) {
        User currentUser = getCurrentUser();
        Page<Payment> payments = paymentRepository.findByUserId(currentUser.getId(), 
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<PaymentResponse> responsePage = payments.map(this::convertToPaymentResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    public SubscriptionResponse getSubscriptionByCommunity(UUID communityId) {
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByCommunityId(communityId)
                .orElseThrow(() -> new BadRequestException("No active subscription found for community"));
        return convertToSubscriptionResponse(subscription);
    }

    @Override
    @Transactional
    public SubscriptionResponse createTrialIfEligible() {
        User currentUser = getCurrentUser();
        if (isUserSubscribed(currentUser.getId())) {
            throw new BadRequestException("User already has an active subscription or trial");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime trialEnd = now.plusDays(7);

        Subscription subscription = Subscription.builder()
                .community(null)
                .user(currentUser)
                .status(SubscriptionStatus.TRIALING)
                .amount(new java.math.BigDecimal("3.00"))
                .currency("GBP")
                .billingCycle("monthly")
                .trialStartDate(now)
                .trialEndDate(trialEnd)
                .currentPeriodStart(now)
                .currentPeriodEnd(trialEnd)
                .nextBillingDate(trialEnd)
                .build();

        subscription = subscriptionRepository.save(subscription);
        return convertToSubscriptionResponse(subscription);
    }

    @Override
    public SubscriptionResponse getMyActiveSubscription() {
        User currentUser = getCurrentUser();
        Subscription subscription = subscriptionRepository.findActiveSubscriptionByUserId(currentUser.getId())
                .orElseThrow(() -> new BadRequestException("No active subscription found for user"));
        return convertToSubscriptionResponse(subscription);
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }
        
        throw new BadRequestException("Webhook handling not implemented in mock mode");
    }

    @Override
    public boolean isCommunitySubscribed(UUID communityId) {
        return subscriptionRepository.countActiveSubscriptionsByCommunityId(communityId) > 0;
    }

    @Override
    public boolean isUserSubscribed(UUID userId) {
        return subscriptionRepository.countActiveSubscriptionsByUserId(userId) > 0;
    }

    @Override
    @Transactional
    public void processFailedPayment(String paymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new BadRequestException("Payment not found"));

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        // Handle failed payment logic (notifications, retry, etc.)
        log.warn("Payment failed for user {}: {}", payment.getUser().getId(), paymentIntentId);
    }

    @Override
    @Transactional
    public WalletCheckoutSessionResponse createWalletCheckoutSession(WalletCheckoutSessionRequest request) {
        // Stripe SDK is not available in this environment; keep this method as a stub
        // to avoid runtime errors. Once stripe-java is added to the classpath and
        // configured, this method can be updated to call the real Stripe API.
        throw new BadRequestException("Stripe SDK is not available - configure Stripe to enable Checkout");
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }
        
        throw new BadRequestException("Invalid authentication principal");
    }

    private PaymentResponse convertToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .type(payment.getType())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .description(payment.getDescription())
                .paymentMethod(payment.getPaymentMethod())
                .paymentIntentId(payment.getStripePaymentIntentId())
                .processedAt(payment.getProcessedAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        boolean isInTrial = subscription.getTrialEndDate() != null && now.isBefore(subscription.getTrialEndDate());
        boolean isActive = subscription.getStatus() == SubscriptionStatus.ACTIVE || 
                          subscription.getStatus() == SubscriptionStatus.TRIALING;
        
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .communityId(subscription.getCommunity() != null ? subscription.getCommunity().getId() : null)
                .communityName(subscription.getCommunity() != null ? subscription.getCommunity().getName() : null)
                .stripeSubscriptionId(subscription.getStripeSubscriptionId())
                .status(subscription.getStatus())
                .amount(subscription.getAmount())
                .currency(subscription.getCurrency())
                .billingCycle(subscription.getBillingCycle())
                .trialStartDate(subscription.getTrialStartDate())
                .trialEndDate(subscription.getTrialEndDate())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .nextBillingDate(subscription.getNextBillingDate())
                .canceledAt(subscription.getCanceledAt())
                .createdAt(subscription.getCreatedAt())
                .isInTrial(isInTrial)
                .isActive(isActive)
                .build();
    }
}
