package com.playvora.playvora_api.payment.services;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.payment.dtos.TransactionResponse;
import com.playvora.playvora_api.payment.entities.Payment;
import com.playvora.playvora_api.payment.entities.Payout;
import com.playvora.playvora_api.payment.entities.Transaction;
import com.playvora.playvora_api.payment.enums.TransactionType;
import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.wallet.entities.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public interface ITransactionService {

    PaginatedResponse<TransactionResponse> getCurrentUserTransactions(int page, int size);

    TransactionResponse getCurrentUserTransaction(UUID transactionId);

    Transaction recordWalletTopup(User user,
                                  Wallet wallet,
                                  BigDecimal amount,
                                  String currency,
                                  String description,
                                  String externalReference);

    Transaction recordMatchPayment(User user,
                                   Wallet wallet,
                                   Payment payment,
                                   Match match,
                                   BigDecimal amount,
                                   String currency,
                                   String description);

    Transaction recordRefund(User user,
                             Wallet wallet,
                             BigDecimal amount,
                             String currency,
                             String description,
                             String externalReference);

    Transaction recordPayout(User user,
                             Payout payout,
                             BigDecimal amount,
                             String currency,
                             String description);

    Transaction recordGeneric(User user,
                              Wallet wallet,
                              Payment payment,
                              Payout payout,
                              Match match,
                              TransactionType type,
                              BigDecimal amount,
                              String currency,
                              String description,
                              String externalReference);
}


