package com.playvora.playvora_api.venue.controllers;

import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.venue.dtos.CreateVenueRequest;
import com.playvora.playvora_api.venue.dtos.UpdateVenueRequest;
import com.playvora.playvora_api.venue.dtos.VenueResponse;
import com.playvora.playvora_api.venue.services.IVenueService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Venue", description = "Venue management APIs")
public class VenueController {

    private final IVenueService venueService;

    @PostMapping
    @Operation(summary = "Create venue", description = "Create a new venue for lease")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            @Valid @RequestBody CreateVenueRequest request
    ) {
        VenueResponse venue = venueService.createVenue(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(venue, "Venue created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get venue by ID", description = "Get a venue by its ID")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(
            @Parameter(description = "Venue ID") @PathVariable UUID id
    ) {
        VenueResponse venue = venueService.getVenueById(id);
        return ResponseEntity.ok(ApiResponse.success(venue, "Venue retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update venue", description = "Update an existing venue")
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(
            @Parameter(description = "Venue ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateVenueRequest request
    ) {
        VenueResponse venue = venueService.updateVenue(id, request);
        return ResponseEntity.ok(ApiResponse.success(venue, "Venue updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete venue", description = "Delete a venue")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(
            @Parameter(description = "Venue ID") @PathVariable UUID id
    ) {
        venueService.deleteVenue(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Venue deleted successfully"));
    }

    @GetMapping
    @Operation(summary = "Get all venues", description = "Get all venues with pagination")
    public ResponseEntity<ApiResponse<PaginatedResponse<VenueResponse>>> getAllVenues(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        PaginatedResponse<VenueResponse> venues = venueService.getAllVenues(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(venues, "Venues retrieved successfully"));
    }
}


