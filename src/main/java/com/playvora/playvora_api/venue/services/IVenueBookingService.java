package com.playvora.playvora_api.venue.services;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.venue.dtos.ConfirmVenueBookingPaymentRequest;
import com.playvora.playvora_api.venue.dtos.CreateVenueBookingRequest;
import com.playvora.playvora_api.venue.dtos.VenueBookingResponse;

import java.util.UUID;

public interface IVenueBookingService {

    VenueBookingResponse createBooking(UUID venueId, CreateVenueBookingRequest request);

    VenueBookingResponse confirmPayment(UUID bookingId, ConfirmVenueBookingPaymentRequest request);

    void cancelBooking(UUID bookingId);

    VenueBookingResponse getBookingById(UUID bookingId);

    PaginatedResponse<VenueBookingResponse> getMyBookings(int page, int size);
}


