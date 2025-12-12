package com.playvora.playvora_api.payment.controllers;

import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.match.dtos.MatchNameDTO;
import com.playvora.playvora_api.payment.dtos.PayoutSetupResponse;
import com.playvora.playvora_api.payment.dtos.TransactionResponse;
import com.playvora.playvora_api.payment.entities.Transaction;
import com.playvora.playvora_api.payment.services.IPayoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/payout")
@Tag(name = "Payout", description = "Payout setup and management APIs for community managers")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Slf4j
public class PayoutController {

    private final IPayoutService payoutService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/setup")
    @Operation(
            summary = "Set up payouts for current community manager",
            description = "Creates or reuses a Stripe connected account for the current manager " +
                    "and returns an Account Session client secret for configuring payouts."
    )
    public ResponseEntity<ApiResponse<PayoutSetupResponse>> setupPayout() {
        PayoutSetupResponse response = payoutService.setupPayout();
        String message = response.isAlreadySetup()
                ? "Payout account already existed. New session created."
                : "Payout account created and session initialized.";
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response, message));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{payoutId}/withdraw")
    @Operation(
            summary = "Withdraw a payout",
            description = "Trigger a Stripe payout for the specified payout record, " +
                    "update its status, record a transaction, and send a push notification."
    )
    public ResponseEntity<ApiResponse<TransactionResponse>> withdrawPayout(
            @Parameter(description = "Payout ID") @PathVariable("payoutId") UUID payoutId
    ) {
        Transaction tx = payoutService.withdrawPayout(payoutId);
        TransactionResponse txResponse = toTransactionResponse(tx);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(txResponse, "Payout withdrawn successfully"));
    }

    private TransactionResponse toTransactionResponse(Transaction tx) {
        MatchNameDTO match = null;
        if (tx.getMatch() != null) {
            match = MatchNameDTO.builder()
                    .id(tx.getMatch().getId())
                    .title(tx.getMatch().getTitle())
                    .build();
        }

        return TransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .description(tx.getDescription())
                .externalReference(tx.getExternalReference())
                .match(match)
                .paymentId(tx.getPayment() != null ? tx.getPayment().getId() : null)
                .payoutId(tx.getPayout() != null ? tx.getPayout().getId() : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}


