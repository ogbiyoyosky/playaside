package com.playvora.playvora_api.notification.services;

import com.playvora.playvora_api.notification.dtos.RegisterDeviceTokenRequest;
import com.playvora.playvora_api.user.entities.User;

import java.util.List;
import java.util.Map;

public interface IPushNotificationService {
    void registerDeviceToken(User user, RegisterDeviceTokenRequest request);
    void unregisterDeviceToken(User user, String token);
    void sendPushNotification(String token, String title, String body, Map<String, Object> data);
    void sendPushNotifications(List<String> tokens, String title, String body, Map<String, Object> data);
    void sendPushNotificationToUser(String userId, String title, String body, Map<String, Object> data);
    void sendPushNotificationToUsers(List<String> userIds, String title, String body, Map<String, Object> data);
}

