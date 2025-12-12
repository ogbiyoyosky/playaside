package com.playvora.playvora_api.payment.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.notification.services.IPushNotificationService;
import com.playvora.playvora_api.payment.dtos.TransactionResponse;
import com.playvora.playvora_api.payment.entities.Payment;
import com.playvora.playvora_api.payment.entities.Payout;
import com.playvora.playvora_api.payment.entities.Transaction;
import com.playvora.playvora_api.payment.enums.TransactionType;
import com.playvora.playvora_api.payment.repo.TransactionRepository;
import com.playvora.playvora_api.payment.services.ITransactionService;
import com.playvora.playvora_api.match.dtos.MatchNameDTO;
import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.wallet.entities.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService implements ITransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final IPushNotificationService pushNotificationService;

    @Override
    public PaginatedResponse<TransactionResponse> getCurrentUserTransactions(int page, int size) {
        User currentUser = getCurrentUser();
        Page<Transaction> txPage = transactionRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Page<TransactionResponse> responsePage = txPage.map(this::toResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    @Override
    public TransactionResponse getCurrentUserTransaction(UUID transactionId) {
        User currentUser = getCurrentUser();
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BadRequestException("Transaction not found"));

        if (!transaction.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Transaction not found");
        }

        return toResponse(transaction);
    }

    @Override
    @Transactional
    public Transaction recordWalletTopup(User user,
                                         Wallet wallet,
                                         BigDecimal amount,
                                         String currency,
                                         String description,
                                         String externalReference) {
        Transaction tx = Transaction.builder()
                .user(user)
                .wallet(wallet)
                .type(TransactionType.WALLET_TOPUP)
                .amount(normalizeAmount(TransactionType.WALLET_TOPUP, amount)) // credit
                .currency(currency)
                .description(description != null ? description : "Wallet top-up")
                .externalReference(externalReference)
                .build();

        tx = transactionRepository.save(tx);
        sendPush(user, tx);
        return tx;
    }

    @Override
    @Transactional
    public Transaction recordMatchPayment(User user,
                                          Wallet wallet,
                                          Payment payment,
                                          Match match,
                                          BigDecimal amount,
                                          String currency,
                                          String description) {
        Transaction tx = Transaction.builder()
                .user(user)
                .wallet(wallet)
                .payment(payment)
                .match(match)
                .type(TransactionType.MATCH_PAYMENT)
                .amount(normalizeAmount(TransactionType.MATCH_PAYMENT, amount)) // debit
                .currency(currency)
                .description(description != null ? description : "Match payment")
                .externalReference(payment != null ? payment.getStripePaymentIntentId() : null)
                .build();

        tx = transactionRepository.save(tx);
        sendPush(user, tx);
        return tx;
    }

    @Override
    @Transactional
    public Transaction recordRefund(User user,
                                    Wallet wallet,
                                    BigDecimal amount,
                                    String currency,
                                    String description,
                                    String externalReference) {
        Transaction tx = Transaction.builder()
                .user(user)
                .wallet(wallet)
                .type(TransactionType.REFUND)
                .amount(normalizeAmount(TransactionType.REFUND, amount)) // credit
                .currency(currency)
                .description(description != null ? description : "Refund")
                .externalReference(externalReference)
                .build();

        tx = transactionRepository.save(tx);
        sendPush(user, tx);
        return tx;
    }

    @Override
    @Transactional
    public Transaction recordPayout(User user,
                                    Payout payout,
                                    BigDecimal amount,
                                    String currency,
                                    String description) {
        Transaction tx = Transaction.builder()
                .user(user)
                .payout(payout)
                .match(payout != null ? payout.getMatch() : null)
                .type(TransactionType.PAYOUT)
                .amount(normalizeAmount(TransactionType.PAYOUT, amount)) // credit to manager
                .currency(currency)
                .description(description != null ? description : "Payout")
                .externalReference(null)
                .build();

        tx = transactionRepository.save(tx);
        sendPush(user, tx);
        return tx;
    }

    @Override
    @Transactional
    public Transaction recordGeneric(User user,
                                     Wallet wallet,
                                     Payment payment,
                                     Payout payout,
                                     Match match,
                                     TransactionType type,
                                     BigDecimal amount,
                                     String currency,
                                     String description,
                                     String externalReference) {
        Transaction tx = Transaction.builder()
                .user(user)
                .wallet(wallet)
                .payment(payment)
                .payout(payout)
                .match(match)
                .type(type)
                .amount(normalizeAmount(type, amount))
                .currency(currency)
                .description(description)
                .externalReference(externalReference)
                .build();

        tx = transactionRepository.save(tx);
        sendPush(user, tx);
        return tx;
    }

    private TransactionResponse toResponse(Transaction tx) {
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

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }

        throw new BadRequestException("Invalid authentication principal");
    }

    /**
     * Ensure a consistent sign convention:
     * - Credits (topups, refunds, payouts) are always stored as positive amounts.
     * - Debits (match payments, other outflows) are always stored as negative amounts.
     */
    private BigDecimal normalizeAmount(TransactionType type, BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Transaction amount is required");
        }

        BigDecimal abs = amount.abs();

        return switch (type) {
            case MATCH_PAYMENT -> abs.negate(); // debit
            default -> abs;                     // credit
        };
    }

    private void sendPush(User user, Transaction tx) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("transactionId", tx.getId().toString());
            data.put("type", tx.getType().name());
            data.put("amount", tx.getAmount());
            data.put("currency", tx.getCurrency());

            String title = "New transaction";
            String body = String.format("%s %s %s",
                    tx.getAmount().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
                    tx.getAmount(),
                    tx.getCurrency());

            pushNotificationService.sendPushNotificationToUser(
                    user.getId().toString(),
                    title,
                    body,
                    data
            );
        } catch (Exception ex) {
            log.error("Failed to send transaction push notification for user {}: {}", user.getId(), ex.getMessage(), ex);
        }
    }
}


