package com.playvora.playvora_api.payment.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.config.StripeConfig;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.payment.dtos.CreateSubscriptionRequest;
import com.playvora.playvora_api.payment.dtos.PaymentIntentRequest;
import com.playvora.playvora_api.payment.dtos.PaymentResponse;
import com.playvora.playvora_api.payment.dtos.SavedPaymentMethodResponse;
import com.playvora.playvora_api.payment.dtos.SavePaymentMethodRequest;
import com.playvora.playvora_api.payment.dtos.SubscriptionResponse;
import com.playvora.playvora_api.payment.dtos.UpdatePaymentIntentRequest;
import com.playvora.playvora_api.payment.dtos.WalletCheckoutSessionRequest;
import com.playvora.playvora_api.payment.dtos.WalletCheckoutSessionResponse;
import com.playvora.playvora_api.payment.dtos.ApplePayDomainRequest;
import com.playvora.playvora_api.payment.dtos.ApplePayDomainResponse;
import com.playvora.playvora_api.payment.entities.EventBooking;
import com.playvora.playvora_api.payment.entities.Payment;
import com.playvora.playvora_api.payment.entities.SavedPaymentMethod;
import com.playvora.playvora_api.payment.entities.Subscription;
import com.playvora.playvora_api.payment.enums.PaymentStatus;
import com.playvora.playvora_api.payment.enums.PaymentType;
import com.playvora.playvora_api.payment.enums.SubscriptionStatus;
import com.playvora.playvora_api.payment.repo.EventBookingRepository;
import com.playvora.playvora_api.payment.repo.PaymentRepository;
import com.playvora.playvora_api.payment.repo.SavedPaymentMethodRepository;
import com.playvora.playvora_api.payment.repo.SubscriptionRepository;
import com.playvora.playvora_api.payment.services.IPaymentService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.wallet.entities.Wallet;
import com.playvora.playvora_api.wallet.services.IWalletService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeObject;
import com.stripe.model.ApplePayDomain;
import com.stripe.model.ApplePayDomainCollection;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.ApplePayDomainCreateParams;
import com.stripe.param.ApplePayDomainListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService implements IPaymentService {
    
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EventBookingRepository eventBookingRepository;
    private final SavedPaymentMethodRepository savedPaymentMethodRepository;
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

            // Attach payment method if provided
            if (request.getPaymentMethodId() != null && !request.getPaymentMethodId().isEmpty()) {
                // Verify the payment method belongs to the user
                savedPaymentMethodRepository.findByUserIdAndStripePaymentMethodId(currentUser.getId(), request.getPaymentMethodId())
                        .orElseThrow(() -> new BadRequestException("Payment method not found or does not belong to user"));

                // Ensure the payment method is attached to the Stripe customer
                ensurePaymentMethodAttachedToCustomer(currentUser, request.getPaymentMethodId());

                paramsBuilder.setPaymentMethod(request.getPaymentMethodId());
                paramsBuilder.setConfirm(true); // Auto-confirm if payment method is provided
                paramsBuilder.setOffSession(true); // Allow off-session payments
            }

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
                    .savePaymentMethod(request.isSavePaymentMethod())
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
    public PaymentResponse updatePaymentIntentWithSavedCard(String paymentIntentId, UpdatePaymentIntentRequest request) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }

        User currentUser = getCurrentUser();

        try {
            // Find the payment record
            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                    .orElseThrow(() -> new BadRequestException("Payment not found"));

            // Verify the payment belongs to the current user
            if (!payment.getUser().getId().equals(currentUser.getId())) {
                throw new BadRequestException("Payment does not belong to current user");
            }

            // Verify payment is still pending
            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new BadRequestException("Payment cannot be updated - it has already been processed");
            }

            // Verify the payment method belongs to the user
            savedPaymentMethodRepository.findByUserIdAndStripePaymentMethodId(currentUser.getId(), request.getPaymentMethodId())
                    .orElseThrow(() -> new BadRequestException("Payment method not found or does not belong to user"));

            // Ensure the payment method is attached to the Stripe customer
            ensurePaymentMethodAttachedToCustomer(currentUser, request.getPaymentMethodId());

            log.info("Payment method found and attached for user: {}", paymentIntentId);
            // Retrieve and update the PaymentIntent in Stripe
            PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);

            log.info("Payment intent retrieved: {}", stripePaymentIntent);

            // Update the PaymentIntent with the saved payment method
            com.stripe.param.PaymentIntentUpdateParams updateParams = com.stripe.param.PaymentIntentUpdateParams.builder()
                    .setPaymentMethod(request.getPaymentMethodId())
                    .build();

            stripePaymentIntent = stripePaymentIntent.update(updateParams);

            return convertToPaymentResponse(payment);

        } catch (StripeException e) {
            log.error("Stripe error updating payment intent {}", paymentIntentId, e);
            throw new BadRequestException("Failed to update payment intent with Stripe");
        } catch (Exception e) {
            log.error("Error updating payment intent: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to update payment intent: " + e.getMessage());
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
            payment.setProcessedAt(OffsetDateTime.now());
            payment.setPaymentMethod(stripePaymentIntent.getPaymentMethod());

            payment = paymentRepository.save(payment);

            // Save payment method if requested
            if (payment.isSavePaymentMethod() && stripePaymentIntent.getPaymentMethod() != null) {
                log.info("Saving payment method after payment: {}", stripePaymentIntent.getPaymentMethod());
                savePaymentMethodAfterPayment(payment.getUser(), stripePaymentIntent.getPaymentMethod());
            }

            // If this payment represents a wallet top-up, apply it to the user's wallet.
            // WalletService will already record a WALLET_TOPUP transaction and trigger push.
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

        try {
            // Retrieve the PaymentIntent from Stripe
            PaymentIntent stripePaymentIntent = PaymentIntent.retrieve(paymentIntentId);

            // Check if the payment can be canceled
            String status = stripePaymentIntent.getStatus();
            if ("succeeded".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)) {
                throw new BadRequestException("Payment cannot be canceled - it has already been " +
                    ("succeeded".equalsIgnoreCase(status) ? "completed" : "canceled"));
            }

            // Cancel the PaymentIntent in Stripe
            stripePaymentIntent = stripePaymentIntent.cancel();

            // Find and update the payment record in our database
            Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                    .orElseThrow(() -> new BadRequestException("Payment not found"));

            // Update payment status and processed timestamp
            payment.setStatus(PaymentStatus.CANCELED);
            payment.setProcessedAt(OffsetDateTime.now());

            payment = paymentRepository.save(payment);

            log.info("Payment canceled successfully: {}", paymentIntentId);

            return convertToPaymentResponse(payment);

        } catch (StripeException e) {
            log.error("Stripe error canceling payment intent {}", paymentIntentId, e);
            throw new BadRequestException("Failed to cancel payment with Stripe");
        } catch (Exception e) {
            log.error("Error canceling payment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to cancel payment: " + e.getMessage());
        }
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

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime trialEnd = now.plusDays(7);

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

        String webhookSecret = stripeConfig.getWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new BadRequestException("Stripe webhook secret is not configured");
        }

        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            String eventType = event.getType();

            log.info("Received Stripe webhook event: {}", eventType);

            if ("account.updated".equals(eventType)) {
                handleAccountUpdated(event);
            } else {
                log.debug("Unhandled Stripe webhook event type: {}", eventType);
            }
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            throw new BadRequestException("Invalid Stripe webhook signature");
        } catch (Exception e) {
            log.error("Error handling Stripe webhook", e);
            throw new BadRequestException("Error handling Stripe webhook: " + e.getMessage());
        }
    }

    private void handleAccountUpdated(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> maybeObject = deserializer.getObject();

        if (maybeObject.isEmpty()) {
            log.warn("Unable to deserialize Stripe account.updated event data");
            return;
        }

        StripeObject stripeObject = maybeObject.get();
        if (!(stripeObject instanceof Account account)) {
            log.warn("Stripe account.updated event did not contain an Account object");
            return;
        }

        Account.Capabilities capabilities = account.getCapabilities();
        if (capabilities != null && "active".equalsIgnoreCase(capabilities.getTransfers())) {
            String accountId = account.getId();
            userRepository.findByConnectedAccountId(accountId).ifPresentOrElse(user -> {
                if (!user.isPayoutsEnabled()) {
                    user.setPayoutsEnabled(true);
                    userRepository.save(user);
                    log.info("Marked user {} as payoutsEnabled for Stripe account {}", user.getId(), accountId);
                } else {
                    log.debug("User {} already has payoutsEnabled=true for Stripe account {}", user.getId(), accountId);
                }
            }, () -> log.warn("No user found with connected_account_id {}", accountId));
        } else {
            log.debug("Stripe account {} transfers capability is not active; skipping payouts enable", account.getId());
        }
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

    @Override
    @Transactional
    public SavedPaymentMethodResponse savePaymentMethod(SavePaymentMethodRequest request) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }

        User currentUser = getCurrentUser();

        try {
            // Ensure user has a Stripe customer ID
            String customerId = ensureStripeCustomer(currentUser);

            // Retrieve payment method from Stripe
            PaymentMethod stripePaymentMethod = PaymentMethod.retrieve(request.getPaymentMethodId());

            // Check if payment method is already saved
            if (savedPaymentMethodRepository.existsByUserIdAndStripePaymentMethodId(currentUser.getId(), request.getPaymentMethodId())) {
                throw new BadRequestException("Payment method is already saved");
            }

            // If setting as default, unset any existing default
            if (request.isSetAsDefault()) {
                savedPaymentMethodRepository.unsetDefaultForUser(currentUser.getId());
            }

            // Attach payment method to customer in Stripe
            stripePaymentMethod.attach(com.stripe.param.PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build());

            // Extract card details if it's a card
            String type = stripePaymentMethod.getType();
            String cardBrand = null;
            String cardLast4 = null;
            Integer cardExpMonth = null;
            Integer cardExpYear = null;

            if ("card".equals(type) && stripePaymentMethod.getCard() != null) {
                cardBrand = stripePaymentMethod.getCard().getBrand();
                cardLast4 = stripePaymentMethod.getCard().getLast4();
                cardExpMonth = stripePaymentMethod.getCard().getExpMonth() != null ?
                    stripePaymentMethod.getCard().getExpMonth().intValue() : null;
                cardExpYear = stripePaymentMethod.getCard().getExpYear() != null ?
                    stripePaymentMethod.getCard().getExpYear().intValue() : null;
            }

            // Save payment method in our database
            SavedPaymentMethod savedMethod = SavedPaymentMethod.builder()
                    .user(currentUser)
                    .stripePaymentMethodId(request.getPaymentMethodId())
                    .type(type)
                    .cardBrand(cardBrand)
                    .cardLast4(cardLast4)
                    .cardExpMonth(cardExpMonth)
                    .cardExpYear(cardExpYear)
                    .isDefault(request.isSetAsDefault())
                    .build();

            savedMethod = savedPaymentMethodRepository.save(savedMethod);

            return convertToSavedPaymentMethodResponse(savedMethod);

        } catch (StripeException e) {
            log.error("Stripe error saving payment method", e);
            throw new BadRequestException("Failed to save payment method with Stripe");
        } catch (Exception e) {
            log.error("Error saving payment method: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to save payment method: " + e.getMessage());
        }
    }

    @Override
    public java.util.List<SavedPaymentMethodResponse> getSavedPaymentMethods() {
        User currentUser = getCurrentUser();
        java.util.List<SavedPaymentMethod> methods = savedPaymentMethodRepository
                .findByUserIdOrderByCreatedAtDesc(currentUser.getId());

        return methods.stream()
                .map(this::convertToSavedPaymentMethodResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSavedPaymentMethod(String paymentMethodId) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }

        User currentUser = getCurrentUser();

        try {
            // Find the saved payment method
            SavedPaymentMethod savedMethod = savedPaymentMethodRepository
                    .findByUserIdAndStripePaymentMethodId(currentUser.getId(), paymentMethodId)
                    .orElseThrow(() -> new BadRequestException("Payment method not found"));

            // Detach from Stripe customer
            PaymentMethod stripePaymentMethod = PaymentMethod.retrieve(paymentMethodId);
            stripePaymentMethod.detach();

            // Delete from our database
            savedPaymentMethodRepository.delete(savedMethod);

        } catch (StripeException e) {
            log.error("Stripe error deleting payment method {}", paymentMethodId, e);
            throw new BadRequestException("Failed to delete payment method with Stripe");
        } catch (Exception e) {
            log.error("Error deleting payment method: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to delete payment method: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SavedPaymentMethodResponse setDefaultPaymentMethod(String paymentMethodId) {
        User currentUser = getCurrentUser();

        // Find the payment method
        SavedPaymentMethod savedMethod = savedPaymentMethodRepository
                .findByUserIdAndStripePaymentMethodId(currentUser.getId(), paymentMethodId)
                .orElseThrow(() -> new BadRequestException("Payment method not found"));

        // Unset any existing default
        savedPaymentMethodRepository.unsetDefaultForUser(currentUser.getId());

        // Set this one as default
        savedPaymentMethodRepository.setAsDefault(savedMethod.getId());

        // Refresh and return
        savedMethod = savedPaymentMethodRepository.findById(savedMethod.getId()).get();
        savedMethod.setDefault(true);

        return convertToSavedPaymentMethodResponse(savedMethod);
    }

    /**
     * Save payment method after successful payment, setting as default if it's the first one
     */
    private void savePaymentMethodAfterPayment(User user, String paymentMethodId) {
        try {
            // Check if payment method is already saved
            if (savedPaymentMethodRepository.existsByUserIdAndStripePaymentMethodId(user.getId(), paymentMethodId)) {
                // Already saved, no need to do anything
                return;
            }

            // Retrieve payment method from Stripe to get details
            PaymentMethod stripePaymentMethod = PaymentMethod.retrieve(paymentMethodId);

            // Check if this is the first saved payment method for the user
            boolean hasExistingPaymentMethods = !savedPaymentMethodRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty();
            boolean setAsDefault = !hasExistingPaymentMethods; // Set as default if it's the first one

            // If setting as default, unset any existing default
            if (setAsDefault) {
                savedPaymentMethodRepository.unsetDefaultForUser(user.getId());
            }

            // Extract card details if it's a card
            String type = stripePaymentMethod.getType();
            String cardBrand = null;
            String cardLast4 = null;
            Integer cardExpMonth = null;
            Integer cardExpYear = null;

            if ("card".equals(type) && stripePaymentMethod.getCard() != null) {
                cardBrand = stripePaymentMethod.getCard().getBrand();
                cardLast4 = stripePaymentMethod.getCard().getLast4();
                cardExpMonth = stripePaymentMethod.getCard().getExpMonth() != null ?
                    stripePaymentMethod.getCard().getExpMonth().intValue() : null;
                cardExpYear = stripePaymentMethod.getCard().getExpYear() != null ?
                    stripePaymentMethod.getCard().getExpYear().intValue() : null;
            }

            // Save payment method in our database
            SavedPaymentMethod savedMethod = SavedPaymentMethod.builder()
                    .user(user)
                    .stripePaymentMethodId(paymentMethodId)
                    .type(type)
                    .cardBrand(cardBrand)
                    .cardLast4(cardLast4)
                    .cardExpMonth(cardExpMonth)
                    .cardExpYear(cardExpYear)
                    .isDefault(setAsDefault)
                    .build();

            savedPaymentMethodRepository.save(savedMethod);

            log.info("Payment method {} saved for user {} (default: {})", paymentMethodId, user.getId(), setAsDefault);

        } catch (Exception e) {
            log.error("Error saving payment method after payment for user {}: {}", user.getId(), e.getMessage(), e);
            // Don't throw exception here as the payment was successful, just log the error
        }
    }

    private void ensurePaymentMethodAttachedToCustomer(User user, String paymentMethodId) throws StripeException {
        // Get or create Stripe customer
        String customerId = ensureStripeCustomer(user);

        // Retrieve the payment method from Stripe
        PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);

        // Check if the payment method is already attached to the customer
        if (paymentMethod.getCustomer() != null && paymentMethod.getCustomer().equals(customerId)) {
            // Already attached to the correct customer
            return;
        }

        // Attach the payment method to the customer
        paymentMethod.attach(com.stripe.param.PaymentMethodAttachParams.builder()
                .setCustomer(customerId)
                .build());

        log.info("Attached payment method {} to customer {} for user {}", paymentMethodId, customerId, user.getId());
    }

    private String ensureStripeCustomer(User user) throws StripeException {
        if (user.getStripeCustomerId() != null) {
            return user.getStripeCustomerId();
        }

        // Create new Stripe customer
        Customer customer = Customer.create(com.stripe.param.CustomerCreateParams.builder()
                .setEmail(user.getEmail())
                .setName(user.getFirstName() + " " + user.getLastName())
                .build());

        // Save customer ID to user
        user.setStripeCustomerId(customer.getId());
        userRepository.save(user);

        return customer.getId();
    }

    private SavedPaymentMethodResponse convertToSavedPaymentMethodResponse(SavedPaymentMethod method) {
        return SavedPaymentMethodResponse.builder()
                .id(method.getId())
                .stripePaymentMethodId(method.getStripePaymentMethodId())
                .type(method.getType())
                .cardBrand(method.getCardBrand())
                .cardLast4(method.getCardLast4())
                .cardExpMonth(method.getCardExpMonth())
                .cardExpYear(method.getCardExpYear())
                .isDefault(method.isDefault())
                .createdAt(method.getCreatedAt())
                .build();
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
        OffsetDateTime now = OffsetDateTime.now();
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

    @Override
    public ApplePayDomainResponse registerApplePayDomain(ApplePayDomainRequest request) {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }

        try {
            ApplePayDomainCreateParams params = ApplePayDomainCreateParams.builder()
                    .setDomainName(request.getDomainName())
                    .build();

            ApplePayDomain domain = ApplePayDomain.create(params);

            log.info("Apple Pay domain registered: {}", domain.getId());

            return ApplePayDomainResponse.builder()
                    .id(domain.getId())
                    .domainName(domain.getDomainName())
                    .created(domain.getCreated())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error registering Apple Pay domain", e);
            throw new BadRequestException("Failed to register Apple Pay domain with Stripe: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error registering Apple Pay domain: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to register Apple Pay domain: " + e.getMessage());
        }
    }

    @Override
    public java.util.List<ApplePayDomainResponse> listApplePayDomains() {
        if (!stripeConfig.isStripeAvailable()) {
            throw new BadRequestException("Payment processing is currently unavailable");
        }

        try {
            ApplePayDomainListParams params = ApplePayDomainListParams.builder().build();
            ApplePayDomainCollection collection = ApplePayDomain.list(params);

            return collection.getData().stream()
                    .map(domain -> ApplePayDomainResponse.builder()
                            .id(domain.getId())
                            .domainName(domain.getDomainName())
                            .created(domain.getCreated())
                            .build())
                    .collect(java.util.stream.Collectors.toList());

        } catch (StripeException e) {
            log.error("Stripe error listing Apple Pay domains", e);
            throw new BadRequestException("Failed to list Apple Pay domains from Stripe: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error listing Apple Pay domains: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to list Apple Pay domains: " + e.getMessage());
        }
    }
}
