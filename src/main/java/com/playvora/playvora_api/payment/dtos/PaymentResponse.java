package com.playvora.playvora_api.payment.dtos;

import com.playvora.playvora_api.payment.enums.PaymentStatus;
import com.playvora.playvora_api.payment.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {
    private UUID id;
    private PaymentType type;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String paymentMethod;
    private String clientSecret;
    private String paymentIntentId;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
