package com.playvora.playvora_api.payment.entities;

import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.payment.enums.TransactionType;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.wallet.entities.Wallet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Optional link to the user's wallet when this is a wallet-related transaction.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    /**
     * Optional link to a payment record when this transaction is derived from a payment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    /**
     * Optional link to a payout record when this transaction is derived from a payout.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id")
    private Payout payout;

    /**
     * Optional link to a match when this transaction relates to a specific event.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private Match match;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private TransactionType type;

    /**
     * Signed amount, in the wallet currency.
     * Positive for credits (topups, payouts), negative for debits (match payments, refunds out of the wallet).
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /**
     * Human-readable description that can be displayed in the app.
     */
    @Column(name = "description")
    private String description;

    /**
     * Optional external reference (e.g. Stripe payment intent id, payout id, etc.).
     */
    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}


