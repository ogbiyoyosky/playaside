package com.playvora.playvora_api.payment.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplePayDomainResponse {
    private String id;
    private String domainName;
    private Long created;
}

