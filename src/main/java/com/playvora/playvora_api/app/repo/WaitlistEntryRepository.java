package com.playvora.playvora_api.app.repo;

import com.playvora.playvora_api.app.entities.WaitlistEntry;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {

    Optional<WaitlistEntry> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}



