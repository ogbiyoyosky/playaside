package com.playvora.playvora_api.payment.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WalletCheckoutSessionResponse {

    /**
     * Stripe Checkout Session ID used by Stripe.js redirectToCheckout.
     */
    private String sessionId;
}


