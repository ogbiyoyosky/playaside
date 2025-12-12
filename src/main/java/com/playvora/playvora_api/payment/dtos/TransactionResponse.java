package com.playvora.playvora_api.payment.dtos;

import com.playvora.playvora_api.match.dtos.MatchNameDTO;
import com.playvora.playvora_api.payment.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private UUID id;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String externalReference;
    private MatchNameDTO match;
    private UUID paymentId;
    private UUID payoutId;
    private OffsetDateTime createdAt;
}


