package com.playvora.playvora_api.payment.controllers;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.config.StripeConfig;
import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.utils.CurrencyMapper;
import com.playvora.playvora_api.payment.dtos.CreateSubscriptionRequest;
import com.playvora.playvora_api.payment.dtos.PaymentIntentRequest;
import com.playvora.playvora_api.payment.dtos.PaymentResponse;
import com.playvora.playvora_api.payment.dtos.SubscriptionResponse;
import com.playvora.playvora_api.payment.dtos.WalletCheckoutSessionRequest;
import com.playvora.playvora_api.payment.dtos.WalletCheckoutSessionResponse;
import com.playvora.playvora_api.payment.services.IPaymentService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/payments")
@Tag(name = "Payment", description = "Payment and subscription management APIs")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final IPaymentService paymentService;
    private final StripeConfig stripeConfig;
    private final UserRepository userRepository;

    @PostMapping("/intent")
    @Operation(summary = "Create payment intent", description = "Create a payment intent for Apple Pay, Google Pay, or card payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPaymentIntent(@Valid @RequestBody PaymentIntentRequest request) {
        PaymentResponse response = paymentService.createPaymentIntent(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Payment intent created successfully"));
    }

    @PostMapping("/confirm/{paymentIntentId}")
    @Operation(summary = "Confirm payment", description = "Confirm a payment intent")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @Parameter(description = "Payment Intent ID") @PathVariable String paymentIntentId) {
        PaymentResponse response = paymentService.confirmPayment(paymentIntentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment confirmed successfully"));
    }

    @PostMapping("/cancel/{paymentIntentId}")
    @Operation(summary = "Cancel payment", description = "Cancel a payment intent")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @Parameter(description = "Payment Intent ID") @PathVariable String paymentIntentId) {
        PaymentResponse response = paymentService.cancelPayment(paymentIntentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment canceled successfully"));
    }

    @PostMapping("/subscriptions")
    @Operation(summary = "Create subscription", description = "Create a monthly subscription for the current user")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
        SubscriptionResponse response = paymentService.createSubscription(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Subscription created successfully"));
    }

    @PostMapping("/subscriptions/trial")
    @Operation(summary = "Start 7-day trial", description = "Create a 7-day trial subscription for the current user if eligible")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> startTrial() {
        SubscriptionResponse response = paymentService.createTrialIfEligible();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Trial started successfully"));
    }

    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    @Operation(summary = "Cancel subscription", description = "Cancel an active subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId) {
        SubscriptionResponse response = paymentService.cancelSubscription(subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Subscription canceled successfully"));
    }

    @PutMapping("/subscriptions/{subscriptionId}/payment-method")
    @Operation(summary = "Update subscription payment method", description = "Update the payment method for a subscription")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> updateSubscriptionPaymentMethod(
            @Parameter(description = "Subscription ID") @PathVariable String subscriptionId,
            @Parameter(description = "Payment Method ID") @RequestParam String paymentMethodId) {
        SubscriptionResponse response = paymentService.updateSubscription(subscriptionId, paymentMethodId);
        return ResponseEntity.ok(ApiResponse.success(response, "Subscription payment method updated successfully"));
    }

    @GetMapping("/my-payments")
    @Operation(summary = "Get user payments", description = "Get all payments for the current user")
    public ResponseEntity<ApiResponse<PaginatedResponse<PaymentResponse>>> getUserPayments(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        PaginatedResponse<PaymentResponse> payments = paymentService.getUserPayments(page, size);
        return ResponseEntity.ok(ApiResponse.success(payments, "User payments retrieved successfully"));
    }

    @GetMapping("/subscriptions/community/{communityId}")
    @Operation(summary = "Get community subscription", description = "Get the active subscription for a community")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getCommunitySubscription(
            @Parameter(description = "Community ID") @PathVariable UUID communityId) {
        SubscriptionResponse response = paymentService.getSubscriptionByCommunity(communityId);
        return ResponseEntity.ok(ApiResponse.success(response, "Community subscription retrieved successfully"));
    }

    @GetMapping("/subscriptions/me")
    @Operation(summary = "Get my subscription", description = "Get the active subscription for the current user")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getMySubscription() {
        SubscriptionResponse response = paymentService.getMyActiveSubscription();
        return ResponseEntity.ok(ApiResponse.success(response, "User subscription retrieved successfully"));
    }

    @GetMapping("/subscriptions/community/{communityId}/status")
    @Operation(summary = "Check subscription status", description = "Check if a community has an active subscription")
    public ResponseEntity<ApiResponse<Boolean>> checkSubscriptionStatus(
            @Parameter(description = "Community ID") @PathVariable UUID communityId) {
        boolean isSubscribed = paymentService.isCommunitySubscribed(communityId);
        return ResponseEntity.ok(ApiResponse.success(isSubscribed, "Subscription status retrieved successfully"));
    }

    @GetMapping("/config")
    @Operation(summary = "Get payment configuration", description = "Get payment configuration including publishable key for Apple Pay")
    public ResponseEntity<ApiResponse<PaymentConfigResponse>> getPaymentConfig() {

        // get the country of the current user
        User currentUser = getCurrentUser();
        String country = currentUser.getCountry();
        String currency = CurrencyMapper.getCurrency(country);
        PaymentConfigResponse config = PaymentConfigResponse.builder()
                .publishableKey(stripeConfig.getPublishableKey())
                .applePayMerchantId("merchant.com.playvora") // Your Apple Pay merchant ID
                .supportedPaymentMethods(stripeConfig.isStripeAvailable() ? 
                        new String[]{"card", "apple_pay", "google_pay"} : 
                        new String[]{})
                .currency(currency)
                .subscriptionAmount("3.00")
                .trialDays(7)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(config, "Payment configuration retrieved successfully"));
    }

    @PostMapping("/webhooks/stripe")
    @Operation(summary = "Stripe webhook", description = "Handle Stripe webhook events")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) {
        try {
            String payload = new String(request.getInputStream().readAllBytes());
            String signature = request.getHeader("Stripe-Signature");
            
            paymentService.handleWebhook(payload, signature);
            
            return ResponseEntity.ok("Webhook processed successfully");
            
        } catch (IOException e) {
            log.error("Error reading webhook payload: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error reading webhook payload");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error processing webhook");
        }
    }

    @PostMapping("/wallet-checkout-session")
    @Operation(summary = "Create Stripe Checkout session for wallet top-up",
            description = "Creates a Stripe Checkout Session using a raw amount instead of a Price ID")
    public ResponseEntity<ApiResponse<WalletCheckoutSessionResponse>> createWalletCheckoutSession(
            @Valid @RequestBody WalletCheckoutSessionRequest request) {
        WalletCheckoutSessionResponse response = paymentService.createWalletCheckoutSession(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Checkout session created successfully"));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }
        throw new BadRequestException("Invalid authentication principal");
    }

    // DTO for payment configuration
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    @lombok.Builder
    public static class PaymentConfigResponse {
        private String publishableKey;
        private String applePayMerchantId;
        private String[] supportedPaymentMethods;
        private String currency;
        private String subscriptionAmount;
        private int trialDays;
    }
}
