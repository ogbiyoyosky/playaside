package com.playvora.playvora_api.venue.controllers;

import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.venue.dtos.ConfirmVenueBookingPaymentRequest;
import com.playvora.playvora_api.venue.dtos.CreateVenueBookingRequest;
import com.playvora.playvora_api.venue.dtos.VenueBookingResponse;
import com.playvora.playvora_api.venue.services.IVenueBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Venue Booking", description = "Venue booking management APIs")
public class VenueBookingController {

    private final IVenueBookingService bookingService;

    @PostMapping("/{venueId}/bookings")
    @Operation(summary = "Create venue booking", description = "Create a new booking for a venue. Booking is pending until payment is confirmed.")
    public ResponseEntity<ApiResponse<VenueBookingResponse>> createBooking(
            @Parameter(description = "Venue ID") @PathVariable UUID venueId,
            @Valid @RequestBody CreateVenueBookingRequest request
    ) {
        VenueBookingResponse booking = bookingService.createBooking(venueId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(booking, "Venue booking created (pending payment)"));
    }

    @PostMapping("/bookings/{bookingId}/confirm-payment")
    @Operation(summary = "Confirm venue booking payment", description = "Mark a venue booking as paid and confirmed. Should be called after successful payment.")
    public ResponseEntity<ApiResponse<VenueBookingResponse>> confirmPayment(
            @Parameter(description = "Booking ID") @PathVariable UUID bookingId,
            @Valid @RequestBody ConfirmVenueBookingPaymentRequest request
    ) {
        VenueBookingResponse booking = bookingService.confirmPayment(bookingId, request);
        return ResponseEntity.ok(ApiResponse.success(booking, "Venue booking payment confirmed and booking finalized"));
    }

    @DeleteMapping("/bookings/{bookingId}")
    @Operation(summary = "Cancel booking", description = "Cancel a venue booking")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @Parameter(description = "Booking ID") @PathVariable UUID bookingId
    ) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(null, "Venue booking canceled successfully"));
    }

    @GetMapping("/bookings/{bookingId}")
    @Operation(summary = "Get booking by ID", description = "Get a single booking by ID (only for the booking user)")
    public ResponseEntity<ApiResponse<VenueBookingResponse>> getBookingById(
            @Parameter(description = "Booking ID") @PathVariable UUID bookingId
    ) {
        VenueBookingResponse booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(ApiResponse.success(booking, "Venue booking retrieved successfully"));
    }

    @GetMapping("/my-bookings")
    @Operation(summary = "Get my bookings", description = "Get bookings for the current user")
    public ResponseEntity<ApiResponse<PaginatedResponse<VenueBookingResponse>>> getMyBookings(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
    ) {
        PaginatedResponse<VenueBookingResponse> bookings = bookingService.getMyBookings(page, size);
        return ResponseEntity.ok(ApiResponse.success(bookings, "Venue bookings retrieved successfully"));
    }
}


