package com.playvora.playvora_api.venue.dtos;

import com.playvora.playvora_api.venue.enums.RentType;
import com.playvora.playvora_api.venue.enums.VenueType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateVenueRequest {

    @NotBlank(message = "Venue name is required")
    @Size(max = 255, message = "Venue name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Venue description is required")
    private String description;

    @NotNull(message = "Venue type is required")
    private VenueType venueType;

    @Size(max = 255, message = "Venue image URL must not exceed 255 characters")
    private String venueImageUrl;

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

    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;

    @NotNull(message = "Opening time is required")
    private LocalTime openingTime;

    @NotNull(message = "Closing time is required")
    private LocalTime closingTime;

    @NotNull(message = "Open Monday flag is required")
    private Boolean openMonday;

    @NotNull(message = "Open Tuesday flag is required")
    private Boolean openTuesday;

    @NotNull(message = "Open Wednesday flag is required")
    private Boolean openWednesday;

    @NotNull(message = "Open Thursday flag is required")
    private Boolean openThursday;

    @NotNull(message = "Open Friday flag is required")
    private Boolean openFriday;

    @NotNull(message = "Open Saturday flag is required")
    private Boolean openSaturday;

    @NotNull(message = "Open Sunday flag is required")
    private Boolean openSunday;

    @NotNull(message = "Rent type is required")
    private RentType rentType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than 0")
    private BigDecimal pricePerHour;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price per day must be greater than 0")
    private BigDecimal pricePerDay;

    private Integer maxRentHours;

    private Integer maxRentDays;
}


