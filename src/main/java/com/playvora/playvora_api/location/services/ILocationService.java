package com.playvora.playvora_api.location.services;

import com.playvora.playvora_api.location.dtos.LocationResponse;

public interface ILocationService {
    LocationResponse getLocationByPostcode(String postcode);
}

