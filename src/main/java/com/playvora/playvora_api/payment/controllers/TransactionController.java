package com.playvora.playvora_api.payment.controllers;

import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.payment.dtos.TransactionResponse;
import com.playvora.playvora_api.payment.services.ITransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/transactions")
@Tag(name = "Transactions", description = "Transaction history APIs")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final ITransactionService transactionService;

    @GetMapping
    @Operation(summary = "Get current user transactions", description = "Returns paginated list of transactions for the authenticated user")
    public ResponseEntity<ApiResponse<PaginatedResponse<TransactionResponse>>> getMyTransactions(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
    ) {
        PaginatedResponse<TransactionResponse> transactions = transactionService.getCurrentUserTransactions(page, size);
        return ResponseEntity.ok(ApiResponse.success(transactions, "Transactions retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single transaction", description = "Returns details of a single transaction belonging to the current user")
    public ResponseEntity<ApiResponse<TransactionResponse>> getMyTransaction(
            @Parameter(description = "Transaction ID") @PathVariable("id") UUID id
    ) {
        TransactionResponse tx = transactionService.getCurrentUserTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(tx, "Transaction retrieved successfully"));
    }
}


