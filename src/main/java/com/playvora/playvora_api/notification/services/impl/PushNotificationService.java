package com.playvora.playvora_api.notification.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.notification.dtos.RegisterDeviceTokenRequest;
import com.playvora.playvora_api.notification.entities.DeviceToken;
import com.playvora.playvora_api.notification.repo.DeviceTokenRepository;
import com.playvora.playvora_api.notification.services.IPushNotificationService;
import com.playvora.playvora_api.user.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService implements IPushNotificationService {

    private static final String EXPO_PUSH_API_URL = "https://exp.host/--/api/v2/push/send";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final DeviceTokenRepository deviceTokenRepository;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${expo.push.enabled:true}")
    private boolean pushEnabled;

    @Override
    @Transactional
    public void registerDeviceToken(User user, RegisterDeviceTokenRequest request) {
        // Check if token already exists for this user
        Optional<DeviceToken> existingToken = deviceTokenRepository.findByUserAndToken(user, request.getToken());
        
        if (existingToken.isPresent()) {
            DeviceToken token = existingToken.get();
            // Reactivate if it was deactivated
            if (!token.getIsActive()) {
                token.setIsActive(true);
                token.setDeviceType(request.getDeviceType().name());
                token.setDeviceId(request.getDeviceId());
                deviceTokenRepository.save(token);
                log.info("Reactivated device token for user {}", user.getId());
            } else {
                // Update device info if provided
                if (request.getDeviceType() != null) {
                    token.setDeviceType(request.getDeviceType().name());
                }
                if (request.getDeviceId() != null) {
                    token.setDeviceId(request.getDeviceId());
                }
                deviceTokenRepository.save(token);
                log.info("Updated device token for user {}", user.getId());
            }
        } else {
            // Create new token
            DeviceToken deviceToken = DeviceToken.builder()
                    .user(user)
                    .token(request.getToken())
                    .deviceType(request.getDeviceType().name())
                    .deviceId(request.getDeviceId())
                    .isActive(true)
                    .build();
            deviceTokenRepository.save(deviceToken);
            log.info("Registered new device token for user {}", user.getId());
        }
    }

    @Override
    @Transactional
    public void unregisterDeviceToken(User user, String token) {
        deviceTokenRepository.deactivateToken(user, token);
        log.info("Deactivated device token for user {}", user.getId());
    }

    @Override
    public void sendPushNotification(String token, String title, String body, Map<String, Object> data) {
        sendPushNotifications(Collections.singletonList(token), title, body, data);
    }

    @Override
    public void sendPushNotifications(List<String> tokens, String title, String body, Map<String, Object> data) {
        if (!pushEnabled) {
            log.debug("Push notifications are disabled");
            return;
        }

        if (tokens == null || tokens.isEmpty()) {
            log.warn("No tokens provided for push notification");
            return;
        }

        try {
            // Expo push API expects either a single message object or an *array* of messages,
            // NOT an object wrapping the messages. We therefore send the list of messages directly.
            List<Map<String, Object>> messages = tokens.stream()
                    .map(token -> {
                        Map<String, Object> message = new HashMap<>();
                        message.put("to", token);
                        message.put("sound", "whistle.wav");
                        message.put("priority", "high"); // High priority for maximum alertness
                        message.put("title", title);
                        message.put("body", body);
                        
                        // Android-specific settings for loud sound
                        Map<String, Object> androidConfig = new HashMap<>();
                        androidConfig.put("priority", "high");
                        androidConfig.put("sound", "default");
                        androidConfig.put("channelId", "default"); // Use default notification channel
                        message.put("android", androidConfig);
                        
                        // iOS-specific settings
                        Map<String, Object> iosConfig = new HashMap<>();
                        iosConfig.put("sound", "whistle.wav");
                        iosConfig.put("badge", 1);
                        message.put("ios", iosConfig);
                        
                        if (data != null && !data.isEmpty()) {
                            message.put("data", data);
                        }
                        return message;
                    })
                    .collect(Collectors.toList());

            String jsonBody = objectMapper.writeValueAsString(messages);
            RequestBody bodyRequest = RequestBody.create(jsonBody, JSON);

            Request request = new Request.Builder()
                    .url(EXPO_PUSH_API_URL)
                    .post(bodyRequest)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Encoding", "gzip, deflate")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Failed to send push notification. Response code: {}, Body: {}", 
                            response.code(), response.body() != null ? response.body().string() : "No body");
                    throw new BadRequestException("Failed to send push notification");
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                log.info("Push notification sent successfully. Response: {}", responseBody);
            }
        } catch (IOException e) {
            log.error("Error sending push notification", e);
            throw new BadRequestException("Error sending push notification: " + e.getMessage());
        }
    }

    @Override
    public void sendPushNotificationToUser(String userId, String title, String body, Map<String, Object> data) {
        UUID userIdUUID = UUID.fromString(userId);
        List<String> tokens = deviceTokenRepository.findByUserIdAndIsActiveTrue(userIdUUID)
                .stream()
                .map(DeviceToken::getToken)
                .collect(Collectors.toList());

        if (!tokens.isEmpty()) {
            sendPushNotifications(tokens, title, body, data);
        } else {
            log.debug("No active device tokens found for user {}", userId);
        }
    }

    @Override
    public void sendPushNotificationToUsers(List<String> userIds, String title, String body, Map<String, Object> data) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("No user IDs provided for push notification");
            return;
        }

        List<UUID> userIdUUIDs = userIds.stream()
                .map(UUID::fromString)
                .collect(java.util.stream.Collectors.toList());
        List<String> tokens = deviceTokenRepository.findByUserIdsAndIsActiveTrue(userIdUUIDs)
                .stream()
                .map(DeviceToken::getToken)
                .collect(Collectors.toList());

        if (!tokens.isEmpty()) {
            sendPushNotifications(tokens, title, body, data);
        } else {
            log.debug("No active device tokens found for users {}", userIds);
        }
    }
}

