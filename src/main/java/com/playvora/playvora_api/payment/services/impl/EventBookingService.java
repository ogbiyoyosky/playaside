package com.playvora.playvora_api.payment.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.payment.dtos.EventBookingResponse;
import com.playvora.playvora_api.payment.entities.EventBooking;
import com.playvora.playvora_api.payment.repo.EventBookingRepository;
import com.playvora.playvora_api.payment.services.IEventBookingService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBookingService implements IEventBookingService {

    private final EventBookingRepository eventBookingRepository;
    private final UserRepository userRepository;

    @Override
    public PaginatedResponse<EventBookingResponse> getMyBookings(LocalDate from, LocalDate to, int page, int size) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookedAt"));

        OffsetDateTime fromDateTime = null;
        OffsetDateTime toDateTime = null;

        if (from != null) {
            fromDateTime = from.atStartOfDay().atOffset(ZoneOffset.UTC);
        }
        if (to != null) {
            // inclusive end of day
            toDateTime = to.atTime(23, 59, 59, 999_000_000).atOffset(ZoneOffset.UTC);
        }

        Page<EventBooking> bookingsPage;
        if (fromDateTime != null && toDateTime != null) {
            bookingsPage = eventBookingRepository.findByUserAndBookedAtBetween(currentUser, fromDateTime, toDateTime, pageable);
        } else if (fromDateTime != null) {
            bookingsPage = eventBookingRepository.findByUserAndBookedAtGreaterThanEqual(currentUser, fromDateTime, pageable);
        } else if (toDateTime != null) {
            bookingsPage = eventBookingRepository.findByUserAndBookedAtLessThanEqual(currentUser, toDateTime, pageable);
        } else {
            bookingsPage = eventBookingRepository.findByUser(currentUser, pageable);
        }

        Page<EventBookingResponse> responsePage = bookingsPage.map(this::toResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    private EventBookingResponse toResponse(EventBooking booking) {
        return EventBookingResponse.builder()
                .id(booking.getId())
                .paymentId(booking.getPayment() != null ? booking.getPayment().getId() : null)
                .matchId(booking.getMatch() != null ? booking.getMatch().getId() : null)
                .userId(booking.getUser().getId())
                .amount(booking.getAmount())
                .currency(booking.getCurrency())
                .reference(booking.getReference())
                .bookedAt(booking.getBookedAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
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


