package com.playvora.playvora_api.app;

import com.playvora.playvora_api.app.dtos.JoinWaitlistRequest;
import com.playvora.playvora_api.app.entities.WaitlistEntry;
import com.playvora.playvora_api.app.repo.WaitlistEntryRepository;
import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.services.IMailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@Tag(name = "Application", description = "General application endpoints")
@RequiredArgsConstructor
public class AppController {

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final IMailService mailService;

    @GetMapping
    @Operation(summary = "Root endpoint", description = "Get welcome message at root path")
    public ResponseEntity<ApiResponse<String>> getAppInfo() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Welcome to Playvora API"));
    }

    @GetMapping(path = "/api/v1")
    @Operation(summary = "API v1 endpoint", description = "Get welcome message for API v1")
    public ResponseEntity<ApiResponse<String>> getApp() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Welcome to Playvora API"));
    }

    @PostMapping(path = "/api/v1/waitlist")
    @Operation(summary = "Join waitlist", description = "Accept an email address, store it, and send a waitlist confirmation email")
    public ResponseEntity<ApiResponse<Void>> joinWaitlist(@Valid @RequestBody JoinWaitlistRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (waitlistEntryRepository.existsByEmailIgnoreCase(email)) {
            // Idempotent behavior: do not fail if already on the waitlist
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "You are already on the waitlist"));
        }

        WaitlistEntry entry = WaitlistEntry.builder()
                .email(email)
                .build();

        try {
            waitlistEntryRepository.save(entry);
        } catch (Exception exception) {
            throw new BadRequestException("Unable to join waitlist. Please try again later.");
        }

        mailService.sendWaitlistConfirmationEmail(email);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Successfully joined the waitlist"));
    }
}