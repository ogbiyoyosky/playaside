package com.playvora.playvora_api.payment.repo;

import com.playvora.playvora_api.payment.entities.Transaction;
import com.playvora.playvora_api.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Page<Transaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}


