package com.playvora.playvora_api.payment.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SavedPaymentMethodResponse {
    private UUID id;
    private String stripePaymentMethodId;
    private String type; // card, apple_pay, google_pay, etc.
    private String cardBrand; // visa, mastercard, amex, etc. (for cards)
    private String cardLast4; // last 4 digits of card
    private Integer cardExpMonth; // expiration month (for cards)
    private Integer cardExpYear; // expiration year (for cards)
    private boolean isDefault;
    private OffsetDateTime createdAt;

    public String getDisplayName() {
        if ("card".equals(type) && cardBrand != null && cardLast4 != null) {
            return cardBrand.substring(0, 1).toUpperCase() + cardBrand.substring(1) + " •••• " + cardLast4;
        }
        return type;
    }
}