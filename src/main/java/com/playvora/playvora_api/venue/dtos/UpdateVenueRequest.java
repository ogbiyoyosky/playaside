package com.playvora.playvora_api.venue.dtos;

import com.playvora.playvora_api.venue.enums.RentType;
import com.playvora.playvora_api.venue.enums.VenueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateVenueRequest {

    private Optional<String> name;
    private Optional<String> description;
    private Optional<VenueType> venueType;
    private Optional<String> venueImageUrl;
    private Optional<String> address;
    private Optional<String> city;
    private Optional<String> province;
    private Optional<String> country;
    private Optional<String> postCode;
    private Optional<BigDecimal> latitude;
    private Optional<BigDecimal> longitude;
    private Optional<LocalTime> openingTime;
    private Optional<LocalTime> closingTime;
    private Optional<Boolean> openMonday;
    private Optional<Boolean> openTuesday;
    private Optional<Boolean> openWednesday;
    private Optional<Boolean> openThursday;
    private Optional<Boolean> openFriday;
    private Optional<Boolean> openSaturday;
    private Optional<Boolean> openSunday;
    private Optional<RentType> rentType;
    private Optional<BigDecimal> pricePerHour;
    private Optional<BigDecimal> pricePerDay;
    private Optional<Integer> maxRentHours;
    private Optional<Integer> maxRentDays;
}


