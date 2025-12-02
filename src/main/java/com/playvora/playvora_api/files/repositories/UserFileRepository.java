package com.playvora.playvora_api.files.repositories;

import com.playvora.playvora_api.files.entities.UserFile;
import com.playvora.playvora_api.user.entities.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFileRepository extends JpaRepository<UserFile, UUID> {
    List<UserFile> findAllByUserOrderByUploadedAtDesc(User user);

    Optional<UserFile> findByUserAndFileName(User user, String fileName);

    Optional<UserFile> findByUserIdAndFileName(UUID userId, String fileName);

    boolean existsByUserAndFileName(User user, String fileName);
}

