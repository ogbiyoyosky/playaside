package com.playvora.playvora_api.payment.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplePayDomainRequest {
    @NotBlank(message = "Domain name is required")
    private String domainName;
}

