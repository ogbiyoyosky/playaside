package com.playvora.playvora_api.location.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {
    private String postcode;
    private Double longitude;
    private Double latitude;
    private String country;
    private String region;
}

