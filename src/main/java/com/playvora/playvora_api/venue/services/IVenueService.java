package com.playvora.playvora_api.venue.services;

import com.playvora.playvora_api.common.dto.PaginatedResponse;
import com.playvora.playvora_api.venue.dtos.CreateVenueRequest;
import com.playvora.playvora_api.venue.dtos.UpdateVenueRequest;
import com.playvora.playvora_api.venue.dtos.VenueResponse;

import java.util.UUID;

public interface IVenueService {

    VenueResponse createVenue(CreateVenueRequest request);

    VenueResponse updateVenue(UUID id, UpdateVenueRequest request);

    void deleteVenue(UUID id);

    VenueResponse getVenueById(UUID id);

    PaginatedResponse<VenueResponse> getAllVenues(int page, int size, String sortBy, String sortDirection);
}


