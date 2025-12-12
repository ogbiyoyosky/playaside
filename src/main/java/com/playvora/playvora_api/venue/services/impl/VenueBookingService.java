package com.playvora.playvora_api.venue.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.services.IMailService;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.venue.dtos.ConfirmVenueBookingPaymentRequest;
import com.playvora.playvora_api.venue.dtos.CreateVenueBookingRequest;
import com.playvora.playvora_api.venue.dtos.VenueBookingResponse;
import com.playvora.playvora_api.venue.entities.Venue;
import com.playvora.playvora_api.venue.entities.VenueBooking;
import com.playvora.playvora_api.venue.enums.RentType;
import com.playvora.playvora_api.venue.enums.VenueBookingStatus;
import com.playvora.playvora_api.venue.repo.VenueBookingRepository;
import com.playvora.playvora_api.venue.repo.VenueRepository;
import com.playvora.playvora_api.venue.services.IVenueBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueBookingService implements IVenueBookingService {

    private final VenueRepository venueRepository;
    private final VenueBookingRepository venueBookingRepository;
    private final UserRepository userRepository;
    private final IMailService mailService;

    @Override
    @Transactional
    public VenueBookingResponse createBooking(UUID venueId, CreateVenueBookingRequest request) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new BadRequestException("Venue not found"));

        User user = getCurrentUser();

        OffsetDateTime start = request.getStartTime();
        OffsetDateTime end = request.getEndTime();

        if (!end.isAfter(start)) {
            throw new BadRequestException("End time must be after start time");
        }

        validateWithinOpeningHours(venue, start, end);
        validateMaxDuration(venue, start, end);

        // Prevent double-booking only on confirmed bookings
        boolean overlap = venueBookingRepository.existsOverlappingBooking(
                venue.getId(),
                VenueBookingStatus.CONFIRMED,
                start,
                end
        );
        if (overlap) {
            throw new BadRequestException("This venue is already booked for the selected time range");
        }

        BigDecimal totalPrice = calculateTotalPrice(venue, start, end);

        VenueBooking booking = VenueBooking.builder()
                .venue(venue)
                .user(user)
                .startTime(start)
                .endTime(end)
                .rentType(venue.getRentType())
                .totalPrice(totalPrice)
                .status(VenueBookingStatus.PENDING_PAYMENT)
                .build();

        VenueBooking saved = venueBookingRepository.save(booking);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public VenueBookingResponse confirmPayment(UUID bookingId, ConfirmVenueBookingPaymentRequest request) {
        VenueBooking booking = venueBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Booking not found"));

        User currentUser = getCurrentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not allowed to confirm this booking");
        }

        if (booking.getStatus() == VenueBookingStatus.CONFIRMED) {
            return toResponse(booking);
        }

        if (booking.getStatus() == VenueBookingStatus.CANCELED) {
            throw new BadRequestException("Cannot confirm a canceled booking");
        }

        booking.setPaymentReference(request.getPaymentReference());
        booking.setStatus(VenueBookingStatus.CONFIRMED);

        VenueBooking updated = venueBookingRepository.save(booking);

        // Send confirmation email
        try {
            mailService.sendVenueBookingConfirmation(currentUser, updated);
        } catch (Exception e) {
            log.error("Failed to send venue booking confirmation email: {}", e.getMessage());
        }

        return toResponse(updated);
    }

    @Override
    @Transactional
    public void cancelBooking(UUID bookingId) {
        VenueBooking booking = venueBookingRepository.findById(bookingId)
            .orElseThrow(() -> new BadRequestException("Booking not found"));

        User currentUser = getCurrentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not allowed to cancel this booking");
        }

        if (booking.getStatus() == VenueBookingStatus.CANCELED) {
            return;
        }

        booking.setStatus(VenueBookingStatus.CANCELED);
        venueBookingRepository.save(booking);
    }

    @Override
    public VenueBookingResponse getBookingById(UUID bookingId) {
        VenueBooking booking = venueBookingRepository.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Booking not found"));

        User currentUser = getCurrentUser();
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not allowed to view this booking");
        }

        return toResponse(booking);
    }

    @Override
    public PaginatedResponse<VenueBookingResponse> getMyBookings(int page, int size) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VenueBooking> bookings = venueBookingRepository.findByUserId(currentUser.getId(), pageable);
        Page<VenueBookingResponse> responsePage = bookings.map(this::toResponse);

        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    private void validateWithinOpeningHours(Venue venue, OffsetDateTime start, OffsetDateTime end) {
        DayOfWeek dayOfWeek = start.getDayOfWeek();
        if (start.toLocalDate().isBefore(end.toLocalDate())) {
            throw new BadRequestException("Bookings must start and end on the same day");
        }

        boolean openThatDay = switch (dayOfWeek) {
            case MONDAY -> venue.isOpenMonday();
            case TUESDAY -> venue.isOpenTuesday();
            case WEDNESDAY -> venue.isOpenWednesday();
            case THURSDAY -> venue.isOpenThursday();
            case FRIDAY -> venue.isOpenFriday();
            case SATURDAY -> venue.isOpenSaturday();
            case SUNDAY -> venue.isOpenSunday();
        };

        if (!openThatDay) {
            throw new BadRequestException("Venue is closed on the selected day");
        }

        LocalTime opening = venue.getOpeningTime();
        LocalTime closing = venue.getClosingTime();

        if (start.toLocalTime().isBefore(opening) || end.toLocalTime().isAfter(closing)) {
            throw new BadRequestException("Booking time must be within venue opening hours");
        }
    }

    private void validateMaxDuration(Venue venue, OffsetDateTime start, OffsetDateTime end) {
        Duration duration = Duration.between(start, end);

        if (venue.getRentType() == RentType.HOURLY) {
            long hours = duration.toHours();
            if (hours <= 0) {
                throw new BadRequestException("Hourly bookings must be at least 1 hour");
            }
            if (venue.getMaxRentHours() != null && hours > venue.getMaxRentHours()) {
                throw new BadRequestException("Booking exceeds maximum rent hours for this venue");
            }
        } else if (venue.getRentType() == RentType.DAILY) {
            long days = duration.toDays();
            if (days <= 0) {
                throw new BadRequestException("Daily bookings must be at least 1 day");
            }
            if (venue.getMaxRentDays() != null && days > venue.getMaxRentDays()) {
                throw new BadRequestException("Booking exceeds maximum rent days for this venue");
            }
        }
    }

    private BigDecimal calculateTotalPrice(Venue venue, OffsetDateTime start, OffsetDateTime end) {
        Duration duration = Duration.between(start, end);

        if (venue.getRentType() == RentType.HOURLY) {
            long hours = Math.max(1, duration.toHours());
            if (venue.getPricePerHour() == null) {
                throw new BadRequestException("Price per hour is not configured for this venue");
            }
            return venue.getPricePerHour().multiply(BigDecimal.valueOf(hours));
        } else if (venue.getRentType() == RentType.DAILY) {
            long days = Math.max(1, duration.toDays());
            if (venue.getPricePerDay() == null) {
                throw new BadRequestException("Price per day is not configured for this venue");
            }
            return venue.getPricePerDay().multiply(BigDecimal.valueOf(days));
        }

        throw new BadRequestException("Unsupported rent type for this venue");
    }

    private VenueBookingResponse toResponse(VenueBooking booking) {
        return VenueBookingResponse.builder()
                .id(booking.getId())
                .venueId(booking.getVenue().getId())
                .venueName(booking.getVenue().getName())
                .userId(booking.getUser().getId())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .rentType(booking.getRentType())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .paymentReference(booking.getPaymentReference())
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


