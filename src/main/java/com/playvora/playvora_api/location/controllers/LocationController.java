package com.playvora.playvora_api.location.controllers;

import com.playvora.playvora_api.common.dto.ApiResponse;
import com.playvora.playvora_api.location.dtos.LocationResponse;
import com.playvora.playvora_api.location.dtos.PostcodeRequest;
import com.playvora.playvora_api.location.services.ILocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
@Tag(name = "Location", description = "Location and postcode lookup APIs")
public class LocationController {

    private final ILocationService locationService;

    @PostMapping("/postcode")
    @Operation(
        summary = "Get location by postcode",
        description = "Retrieves longitude and latitude for a given UK postcode using the Postcodes.io API"
    )
    public ResponseEntity<ApiResponse<LocationResponse>> getLocationByPostcode(
            @Valid @RequestBody PostcodeRequest request) {
        
        LocationResponse location = locationService.getLocationByPostcode(request.getPostcode());
        
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(location, "Location retrieved successfully"));
    }

    @GetMapping("/postcode/{postcode}")
    @Operation(
        summary = "Get location by postcode (GET)",
        description = "Retrieves longitude and latitude for a given UK postcode using path parameter"
    )
    public ResponseEntity<ApiResponse<LocationResponse>> getLocationByPostcodeGet(
            @PathVariable String postcode) {
        
        LocationResponse location = locationService.getLocationByPostcode(postcode);
        
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(location, "Location retrieved successfully"));
    }
}

