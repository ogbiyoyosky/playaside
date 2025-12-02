package com.playvora.playvora_api.community.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateCommunityRequest {
    @NotBlank(message = "Community name is required")
    @Size(max = 255, message = "Community name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Community description is required")
    @Size(max = 255, message = "Community description must not exceed 255 characters")
    private String description;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 255, message = "City must not exceed 255 characters")
    private String city;

    @NotBlank(message = "Province is required")
    @Size(max = 255, message = "Province must not exceed 255 characters")
    private String province;

    @NotBlank(message = "Country is required")
    @Size(max = 255, message = "Country must not exceed 255 characters")
    private String country;

    @NotBlank(message = "Post code is required")
    @Size(max = 20, message = "Post code must not exceed 20 characters")
    private String postCode;

    @NotBlank(message = "Logo URL is required")
    @Size(max = 255, message = "Logo URL must not exceed 255 characters")
    private String logoUrl;

    @NotBlank(message = "Banner URL is required")
    @Size(max = 255, message = "Banner URL must not exceed 255 characters")
    private String bannerUrl;

    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;
}
