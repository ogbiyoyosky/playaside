package com.playvora.playvora_api.payment.services;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.payment.dtos.CreateSubscriptionRequest;
import com.playvora.playvora_api.payment.dtos.PaymentIntentRequest;
import com.playvora.playvora_api.payment.dtos.PaymentResponse;
import com.playvora.playvora_api.payment.dtos.SubscriptionResponse;
import com.playvora.playvora_api.payment.dtos.WalletCheckoutSessionRequest;
import com.playvora.playvora_api.payment.dtos.WalletCheckoutSessionResponse;

import java.util.UUID;

public interface IPaymentService {
    PaymentResponse createPaymentIntent(PaymentIntentRequest request);
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
}
