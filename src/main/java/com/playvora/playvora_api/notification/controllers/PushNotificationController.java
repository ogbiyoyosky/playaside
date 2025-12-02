package com.playvora.playvora_api.notification.controllers;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.notification.dtos.RegisterDeviceTokenRequest;
import com.playvora.playvora_api.notification.services.IPushNotificationService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Push Notifications", description = "Push notification management APIs")
public class PushNotificationController {

    private final IPushNotificationService pushNotificationService;
    private final UserRepository userRepository;

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/device-token")
    @Operation(summary = "Register device token", description = "Register or update a device token for push notifications")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        User currentUser = getCurrentUser();
        pushNotificationService.registerDeviceToken(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Device token registered successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/device-token/{token}")
    @Operation(summary = "Unregister device token", description = "Unregister a device token for push notifications")
    public ResponseEntity<ApiResponse<Void>> unregisterDeviceToken(
            @Parameter(description = "Device token to unregister") @PathVariable String token) {
        User currentUser = getCurrentUser();
        pushNotificationService.unregisterDeviceToken(currentUser, token);
        return ResponseEntity.ok(ApiResponse.success(null, "Device token unregistered successfully"));
    }

    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/test")
    @Operation(
            summary = "Send test push notification",
            description = "Sends a test push notification to the currently authenticated user using their active device tokens"
    )
    public ResponseEntity<ApiResponse<Void>> sendTestPushNotification() {
        User currentUser = getCurrentUser();

        Map<String, Object> data = new HashMap<>();
        data.put("type", "TEST_NOTIFICATION");
        data.put("message", "This is a test push notification");

        pushNotificationService.sendPushNotificationToUser(
                currentUser.getId().toString(),
                "Playvora Test Notification",
                "This is a test push notification from the API.",
                data
        );

        return ResponseEntity.ok(
                ApiResponse.success(null, "Test push notification attempted (check that you have an active device token)")
        );
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof AppUserDetail userDetail) {
            return userRepository.findByEmail(userDetail.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));
        }

        throw new BadRequestException("Invalid authentication principal");
    }
}

