package com.playvora.playvora_api.payment.entities;

import com.playvora.playvora_api.user.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name = "saved_payment_methods")
public class SavedPaymentMethod {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stripe_payment_method_id", nullable = false, unique = true)
    private String stripePaymentMethodId;

    @Column(name = "type", nullable = false)
    private String type; // card, apple_pay, google_pay, etc.

    @Column(name = "card_brand")
    private String cardBrand; // visa, mastercard, amex, etc. (for cards)

    @Column(name = "card_last4")
    private String cardLast4; // last 4 digits of card

    @Column(name = "card_exp_month")
    private Integer cardExpMonth; // expiration month (for cards)

    @Column(name = "card_exp_year")
    private Integer cardExpYear; // expiration year (for cards)

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

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