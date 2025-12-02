package com.playvora.playvora_api.auth.services.impl;

import com.playvora.playvora_api.auth.entities.PasswordResetToken;
import com.playvora.playvora_api.auth.repo.PasswordResetTokenRepository;
import com.playvora.playvora_api.auth.services.IPasswordResetService;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.services.IMailService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.enums.AuthProvider;
import com.playvora.playvora_api.user.repo.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService implements IPasswordResetService {
    private static final long TOKEN_EXPIRATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IMailService mailService;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public String initiatePasswordReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> optionalUser = userRepository.findByEmail(normalizedEmail);

        if (optionalUser.isEmpty()) {
            log.debug("Password reset requested for non-existing email {}", normalizedEmail);
            return null;
        }

        User user = optionalUser.get();

        if (user.getProvider() != null && user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password reset is only available for local accounts");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(TOKEN_EXPIRATION_MINUTES);

        // Invalidate any existing tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        String rawToken = buildRawToken(user, expiresAt);
        String encodedToken = encodeToken(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(encodedToken)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(now)
                .build();
        passwordResetTokenRepository.save(resetToken);

        sendPasswordResetEmail(user, encodedToken, expiresAt);

        cleanupExpiredTokens(now);
        return encodedToken;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Reset token is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new BadRequestException("New password is required");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        LocalDateTime now = LocalDateTime.now();
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(now)) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        cleanupExpiredTokens(now);
    }

    private void cleanupExpiredTokens(LocalDateTime referenceTime) {
        passwordResetTokenRepository.deleteByExpiresAtBefore(referenceTime);
    }

    private String buildRawToken(User user, LocalDateTime expiresAt) {
        return user.getId() + ":" + user.getEmail() + ":" + UUID.randomUUID() + ":" + expiresAt;
    }

    private String encodeToken(String rawToken) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    private void sendPasswordResetEmail(User user, String token, LocalDateTime expiresAt) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            log.warn("Frontend URL not configured. Skipping password reset email for user {}", user.getEmail());
            return;
        }

        String resetLink = buildResetLink(token);
        mailService.sendPasswordResetEmail(user, resetLink, expiresAt);
    }

    private String buildResetLink(String token) {
        String trimmedBaseUrl = frontendUrl.trim();
        if (trimmedBaseUrl.endsWith("/")) {
            trimmedBaseUrl = trimmedBaseUrl.substring(0, trimmedBaseUrl.length() - 1);
        }
        return trimmedBaseUrl + "/reset-password?token=" + token;
    }
}

