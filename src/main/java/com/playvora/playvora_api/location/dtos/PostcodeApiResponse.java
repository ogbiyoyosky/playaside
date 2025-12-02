package com.playvora.playvora_api.location.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PostcodeApiResponse {
    private Integer status;
    private Result result;
    private String error;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private String postcode;
        private Double longitude;
        private Double latitude;
        private String country;
        private String region;
    }
}

