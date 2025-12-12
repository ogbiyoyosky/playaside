package com.playvora.playvora_api.payment.services;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.payment.dtos.EventBookingResponse;

import java.time.LocalDate;

public interface IEventBookingService {

    /**
     * Get paginated event bookings for the current user, optionally filtered by
     * a date range on bookedAt.
     *
     * @param from optional start date (inclusive)
     * @param to   optional end date (inclusive)
     * @param page zero-based page index
     * @param size page size
     */
    PaginatedResponse<EventBookingResponse> getMyBookings(LocalDate from, LocalDate to, int page, int size);
}


