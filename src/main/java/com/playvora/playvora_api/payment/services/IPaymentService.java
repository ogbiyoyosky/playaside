package com.playvora.playvora_api.payment.services;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
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

import java.util.UUID;

public interface IPaymentService {
    PaymentResponse createPaymentIntent(PaymentIntentRequest request);
    PaymentResponse updatePaymentIntentWithSavedCard(String paymentIntentId, UpdatePaymentIntentRequest request);
    PaymentResponse confirmPayment(String paymentIntentId);
    PaymentResponse cancelPayment(String paymentIntentId);
    SubscriptionResponse createSubscription(CreateSubscriptionRequest request);
    SubscriptionResponse createTrialIfEligible();
    SubscriptionResponse getMyActiveSubscription();
    SubscriptionResponse cancelSubscription(String subscriptionId);
    SubscriptionResponse updateSubscription(String subscriptionId, String paymentMethodId);
    PaginatedResponse<PaymentResponse> getUserPayments(int page, int size);
    SubscriptionResponse getSubscriptionByCommunity(UUID communityId);
    boolean isUserSubscribed(UUID userId);
    void handleWebhook(String payload, String signature);
    boolean isCommunitySubscribed(UUID communityId);
    void processFailedPayment(String paymentIntentId);

    /**
     * Create a Stripe Checkout Session for a wallet top-up using a raw amount
     * instead of a pre-created Price ID.
     */
    WalletCheckoutSessionResponse createWalletCheckoutSession(WalletCheckoutSessionRequest request);

    /**
     * Save a payment method for the current user
     */
    SavedPaymentMethodResponse savePaymentMethod(SavePaymentMethodRequest request);

    /**
     * Get all saved payment methods for the current user
     */
    java.util.List<SavedPaymentMethodResponse> getSavedPaymentMethods();

    /**
     * Delete a saved payment method
     */
    void deleteSavedPaymentMethod(String paymentMethodId);

    /**
     * Set a payment method as the default for the user
     */
    SavedPaymentMethodResponse setDefaultPaymentMethod(String paymentMethodId);

    /**
     * Register an Apple Pay domain with Stripe
     */
    ApplePayDomainResponse registerApplePayDomain(ApplePayDomainRequest request);

    /**
     * List all registered Apple Pay domains
     */
    java.util.List<ApplePayDomainResponse> listApplePayDomains();
}
