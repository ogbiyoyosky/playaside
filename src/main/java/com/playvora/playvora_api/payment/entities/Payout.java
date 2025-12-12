package com.playvora.playvora_api.payment.entities;

import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.match.entities.Match;
import com.playvora.playvora_api.payment.enums.PayoutStatus;
import com.playvora.playvora_api.user.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name = "payouts")
public class Payout {

    @Id
    @GeneratedValue
    private UUID id;

    /**
     * Match/event this payout is associated with.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /**
     * Community that owns the match (helps for reporting).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /**
     * Manager user (community manager / organizer) that will receive this payout.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_user_id", nullable = false)
    private User managerUser;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    @Builder.Default
    private String currency = "GBP";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    /**
     * Date when this payout should be processed (match date + 1 day).
     */
    @Column(name = "scheduled_payout_date", nullable = false)
    private OffsetDateTime scheduledPayoutDate;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "failure_reason")
    private String failureReason;

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


