package com.playvora.playvora_api.payment.repo;

import com.playvora.playvora_api.payment.entities.SavedPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedPaymentMethodRepository extends JpaRepository<SavedPaymentMethod, UUID> {

    List<SavedPaymentMethod> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<SavedPaymentMethod> findByUserIdAndStripePaymentMethodId(UUID userId, String stripePaymentMethodId);

    Optional<SavedPaymentMethod> findByUserIdAndIsDefaultTrue(UUID userId);

    boolean existsByUserIdAndStripePaymentMethodId(UUID userId, String stripePaymentMethodId);

    @Modifying
    @Query("UPDATE SavedPaymentMethod spm SET spm.isDefault = false WHERE spm.user.id = :userId")
    void unsetDefaultForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE SavedPaymentMethod spm SET spm.isDefault = true WHERE spm.id = :paymentMethodId")
    void setAsDefault(@Param("paymentMethodId") UUID paymentMethodId);

    void deleteByUserIdAndStripePaymentMethodId(UUID userId, String stripePaymentMethodId);
}