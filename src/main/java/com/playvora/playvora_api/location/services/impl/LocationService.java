package com.playvora.playvora_api.location.services.impl;

import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.location.dtos.LocationResponse;
import com.playvora.playvora_api.location.dtos.PostcodeApiResponse;
import com.playvora.playvora_api.location.services.ILocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Service
public class LocationService implements ILocationService {

    private static final String POSTCODE_API_BASE_URL = "https://api.postcodes.io/postcodes/";
    private final RestTemplate restTemplate;

    public LocationService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public LocationResponse getLocationByPostcode(String postcode) {
        // Remove spaces and convert to uppercase for API call
        String cleanedPostcode = postcode.replaceAll("\\s+", "").toUpperCase();
        
        try {
            log.info("Fetching location data for postcode: {}", cleanedPostcode);
            
            String url = POSTCODE_API_BASE_URL + cleanedPostcode;
            PostcodeApiResponse apiResponse = restTemplate.getForObject(url, PostcodeApiResponse.class);
            
            if (apiResponse == null || apiResponse.getStatus() != 200 || apiResponse.getResult() == null) {
                log.warn("Invalid postcode: {}", postcode);
                throw new BadRequestException("Invalid postcode");
            }
            
            PostcodeApiResponse.Result result = apiResponse.getResult();
            
            log.info("Successfully retrieved location for postcode: {}", result.getPostcode());
            
            return LocationResponse.builder()
                    .postcode(result.getPostcode())
                    .longitude(result.getLongitude())
                    .latitude(result.getLatitude())
                    .country(result.getCountry())
                    .region(result.getRegion())
                    .build();
                    
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Postcode not found: {}", postcode);
            throw new BadRequestException("Invalid postcode");
        } catch (HttpClientErrorException e) {
            log.error("Error calling postcode API: {}", e.getMessage());
            throw new BadRequestException("Invalid postcode");
        } catch (Exception e) {
            log.error("Unexpected error retrieving postcode: {}", e.getMessage(), e);
            throw new BadRequestException("Error retrieving postcode information");
        }
    }
}

