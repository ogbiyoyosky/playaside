package com.playvora.playvora_api.venue.dtos;

import com.playvora.playvora_api.venue.enums.RentType;
import com.playvora.playvora_api.venue.enums.VenueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VenueResponse {

    private UUID id;
    private String name;
    private String description;
    private VenueType venueType;
    private String venueImageUrl;
    private String address;
    private String city;
    private String province;
    private String country;
    private String postCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private boolean openMonday;
    private boolean openTuesday;
    private boolean openWednesday;
    private boolean openThursday;
    private boolean openFriday;
    private boolean openSaturday;
    private boolean openSunday;
    private RentType rentType;
    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;
    private Integer maxRentHours;
    private Integer maxRentDays;
    private UUID ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


