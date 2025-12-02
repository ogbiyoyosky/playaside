package com.playvora.playvora_api.match.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JoinMatchRequest {

    /**
     * ID of the confirmed payment intent that was used to pay for this match.
     * For paid events this is required and must point to a Payment with SUCCEEDED status.
     * For free events this can be null.
     */
    private String paymentIntentId;
}


