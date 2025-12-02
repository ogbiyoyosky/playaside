package com.playvora.playvora_api.venue.services.impl;

import com.playvora.playvora_api.app.AppUserDetail;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.utils.PaginationUtils;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.user.repo.UserRepository;
import com.playvora.playvora_api.venue.dtos.CreateVenueRequest;
import com.playvora.playvora_api.venue.dtos.UpdateVenueRequest;
import com.playvora.playvora_api.venue.dtos.VenueResponse;
import com.playvora.playvora_api.venue.entities.Venue;
import com.playvora.playvora_api.venue.enums.RentType;
import com.playvora.playvora_api.venue.repo.VenueRepository;
import com.playvora.playvora_api.venue.services.IVenueService;
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
import java.time.LocalTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenueService implements IVenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public VenueResponse createVenue(CreateVenueRequest request) {
        validateTimeRange(request.getOpeningTime(), request.getClosingTime());
        validateOpeningDays(
                request.getOpenMonday(),
                request.getOpenTuesday(),
                request.getOpenWednesday(),
                request.getOpenThursday(),
                request.getOpenFriday(),
                request.getOpenSaturday(),
                request.getOpenSunday()
        );
        validateRentFields(
                request.getRentType(),
                request.getPricePerHour(),
                request.getPricePerDay(),
                request.getMaxRentHours(),
                request.getMaxRentDays()
        );

        User owner = getCurrentUser();

        Venue venue = Venue.builder()
                .name(request.getName())
                .description(request.getDescription())
                .venueType(request.getVenueType())
                .venueImageUrl(request.getVenueImageUrl())
                .address(request.getAddress())
                .city(request.getCity())
                .province(request.getProvince())
                .country(request.getCountry())
                .postCode(request.getPostCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .openMonday(request.getOpenMonday())
                .openTuesday(request.getOpenTuesday())
                .openWednesday(request.getOpenWednesday())
                .openThursday(request.getOpenThursday())
                .openFriday(request.getOpenFriday())
                .openSaturday(request.getOpenSaturday())
                .openSunday(request.getOpenSunday())
                .rentType(request.getRentType())
                .pricePerHour(request.getPricePerHour())
                .pricePerDay(request.getPricePerDay())
                .maxRentHours(request.getMaxRentHours())
                .maxRentDays(request.getMaxRentDays())
                .owner(owner)
                .build();

        Venue saved = venueRepository.save(venue);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public VenueResponse updateVenue(UUID id, UpdateVenueRequest request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Venue not found"));

        // basic ownership check: only owner can update (or later: admins)
        User currentUser = getCurrentUser();
        if (!venue.getOwner().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not allowed to update this venue");
        }

        if (request.getName() != null && request.getName().isPresent()) {
            venue.setName(request.getName().get());
        }
        if (request.getDescription() != null && request.getDescription().isPresent()) {
            venue.setDescription(request.getDescription().get());
        }
        if (request.getVenueType() != null && request.getVenueType().isPresent()) {
            venue.setVenueType(request.getVenueType().get());
        }
        if (request.getVenueImageUrl() != null && request.getVenueImageUrl().isPresent()) {
            venue.setVenueImageUrl(request.getVenueImageUrl().get());
        }
        if (request.getAddress() != null && request.getAddress().isPresent()) {
            venue.setAddress(request.getAddress().get());
        }
        if (request.getCity() != null && request.getCity().isPresent()) {
            venue.setCity(request.getCity().get());
        }
        if (request.getProvince() != null && request.getProvince().isPresent()) {
            venue.setProvince(request.getProvince().get());
        }
        if (request.getCountry() != null && request.getCountry().isPresent()) {
            venue.setCountry(request.getCountry().get());
        }
        if (request.getPostCode() != null && request.getPostCode().isPresent()) {
            venue.setPostCode(request.getPostCode().get());
        }
        if (request.getLatitude() != null && request.getLatitude().isPresent()) {
            venue.setLatitude(request.getLatitude().get());
        }
        if (request.getLongitude() != null && request.getLongitude().isPresent()) {
            venue.setLongitude(request.getLongitude().get());
        }
        if (request.getOpeningTime() != null && request.getOpeningTime().isPresent()) {
            venue.setOpeningTime(request.getOpeningTime().get());
        }
        if (request.getClosingTime() != null && request.getClosingTime().isPresent()) {
            venue.setClosingTime(request.getClosingTime().get());
        }
        if (request.getOpenMonday() != null && request.getOpenMonday().isPresent()) {
            venue.setOpenMonday(request.getOpenMonday().get());
        }
        if (request.getOpenTuesday() != null && request.getOpenTuesday().isPresent()) {
            venue.setOpenTuesday(request.getOpenTuesday().get());
        }
        if (request.getOpenWednesday() != null && request.getOpenWednesday().isPresent()) {
            venue.setOpenWednesday(request.getOpenWednesday().get());
        }
        if (request.getOpenThursday() != null && request.getOpenThursday().isPresent()) {
            venue.setOpenThursday(request.getOpenThursday().get());
        }
        if (request.getOpenFriday() != null && request.getOpenFriday().isPresent()) {
            venue.setOpenFriday(request.getOpenFriday().get());
        }
        if (request.getOpenSaturday() != null && request.getOpenSaturday().isPresent()) {
            venue.setOpenSaturday(request.getOpenSaturday().get());
        }
        if (request.getOpenSunday() != null && request.getOpenSunday().isPresent()) {
            venue.setOpenSunday(request.getOpenSunday().get());
        }
        if (request.getRentType() != null && request.getRentType().isPresent()) {
            venue.setRentType(request.getRentType().get());
        }
        if (request.getPricePerHour() != null && request.getPricePerHour().isPresent()) {
            venue.setPricePerHour(request.getPricePerHour().get());
        }
        if (request.getPricePerDay() != null && request.getPricePerDay().isPresent()) {
            venue.setPricePerDay(request.getPricePerDay().get());
        }
        if (request.getMaxRentHours() != null && request.getMaxRentHours().isPresent()) {
            venue.setMaxRentHours(request.getMaxRentHours().get());
        }
        if (request.getMaxRentDays() != null && request.getMaxRentDays().isPresent()) {
            venue.setMaxRentDays(request.getMaxRentDays().get());
        }

        validateTimeRange(venue.getOpeningTime(), venue.getClosingTime());
        validateOpeningDays(
                venue.isOpenMonday(),
                venue.isOpenTuesday(),
                venue.isOpenWednesday(),
                venue.isOpenThursday(),
                venue.isOpenFriday(),
                venue.isOpenSaturday(),
                venue.isOpenSunday()
        );
        validateRentFields(
                venue.getRentType(),
                venue.getPricePerHour(),
                venue.getPricePerDay(),
                venue.getMaxRentHours(),
                venue.getMaxRentDays()
        );

        Venue updated = venueRepository.save(venue);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteVenue(UUID id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Venue not found"));

        User currentUser = getCurrentUser();
        if (!venue.getOwner().getId().equals(currentUser.getId())) {
            throw new BadRequestException("You are not allowed to delete this venue");
        }

        venueRepository.delete(venue);
    }

    @Override
    public VenueResponse getVenueById(UUID id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Venue not found"));
        return toResponse(venue);
    }

    @Override
    public PaginatedResponse<VenueResponse> getAllVenues(int page, int size, String sortBy, String sortDirection) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDirection)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC,
                sortBy
        );

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Venue> venues = venueRepository.findAll(pageable);
        Page<VenueResponse> responsePage = venues.map(this::toResponse);
        return PaginationUtils.toPaginatedResponse(responsePage);
    }

    private VenueResponse toResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .description(venue.getDescription())
                .venueType(venue.getVenueType())
                .venueImageUrl(venue.getVenueImageUrl())
                .address(venue.getAddress())
                .city(venue.getCity())
                .province(venue.getProvince())
                .country(venue.getCountry())
                .postCode(venue.getPostCode())
                .latitude(venue.getLatitude())
                .longitude(venue.getLongitude())
                .openingTime(venue.getOpeningTime())
                .closingTime(venue.getClosingTime())
                .openMonday(venue.isOpenMonday())
                .openTuesday(venue.isOpenTuesday())
                .openWednesday(venue.isOpenWednesday())
                .openThursday(venue.isOpenThursday())
                .openFriday(venue.isOpenFriday())
                .openSaturday(venue.isOpenSaturday())
                .openSunday(venue.isOpenSunday())
                .rentType(venue.getRentType())
                .pricePerHour(venue.getPricePerHour())
                .pricePerDay(venue.getPricePerDay())
                .maxRentHours(venue.getMaxRentHours())
                .maxRentDays(venue.getMaxRentDays())
                .ownerId(venue.getOwner() != null ? venue.getOwner().getId() : null)
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }

    private void validateTimeRange(LocalTime openingTime, LocalTime closingTime) {
        if (openingTime != null && closingTime != null && !closingTime.isAfter(openingTime)) {
            throw new BadRequestException("Closing time must be after opening time");
        }
    }

    private void validateOpeningDays(
            Boolean monday,
            Boolean tuesday,
            Boolean wednesday,
            Boolean thursday,
            Boolean friday,
            Boolean saturday,
            Boolean sunday
    ) {
        boolean anyOpen = Boolean.TRUE.equals(monday)
                || Boolean.TRUE.equals(tuesday)
                || Boolean.TRUE.equals(wednesday)
                || Boolean.TRUE.equals(thursday)
                || Boolean.TRUE.equals(friday)
                || Boolean.TRUE.equals(saturday)
                || Boolean.TRUE.equals(sunday);

        if (!anyOpen) {
            throw new BadRequestException("At least one opening day must be selected");
        }
    }

    private void validateRentFields(
            RentType rentType,
            BigDecimal pricePerHour,
            BigDecimal pricePerDay,
            Integer maxRentHours,
            Integer maxRentDays
    ) {
        if (rentType == null) {
            throw new BadRequestException("Rent type is required");
        }

        if (rentType == RentType.HOURLY) {
            if (pricePerHour == null) {
                throw new BadRequestException("Price per hour is required for hourly rent type");
            }
            if (maxRentHours == null) {
                throw new BadRequestException("Max rent hours is required for hourly rent type");
            }
            if (pricePerHour.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Price per hour must be greater than 0");
            }
            if (maxRentHours <= 0) {
                throw new BadRequestException("Max rent hours must be greater than 0");
            }
        } else if (rentType == RentType.DAILY) {
            if (pricePerDay == null) {
                throw new BadRequestException("Price per day is required for daily rent type");
            }
            if (maxRentDays == null) {
                throw new BadRequestException("Max rent days is required for daily rent type");
            }
            if (pricePerDay.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Price per day must be greater than 0");
            }
            if (maxRentDays <= 0) {
                throw new BadRequestException("Max rent days must be greater than 0");
            }
        }
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


