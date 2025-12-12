package com.playvora.playvora_api.payment.controllers;

import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.payment.dtos.EventBookingResponse;
import com.playvora.playvora_api.payment.services.IEventBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/event-bookings")
@RequiredArgsConstructor
@Tag(name = "Event Booking", description = "Event booking management APIs")
public class EventBookingController {

    private final IEventBookingService eventBookingService;

    @GetMapping("/my-bookings")
    @Operation(summary = "Get my event bookings", description = "Get paginated event bookings for the current user, optionally filtered by booked date range")
    public ResponseEntity<ApiResponse<PaginatedResponse<EventBookingResponse>>> getMyBookings(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Start date (inclusive) in format yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "End date (inclusive) in format yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        PaginatedResponse<EventBookingResponse> bookings = eventBookingService.getMyBookings(from, to, page, size);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Event bookings retrieved successfully"));
    }
}


