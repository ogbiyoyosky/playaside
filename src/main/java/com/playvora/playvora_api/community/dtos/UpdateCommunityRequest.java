package com.playvora.playvora_api.community.dtos;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateCommunityRequest {
    // Note: @Size validation doesn't work with Optional types
    // Validation will be enforced by database constraints and service layer
    
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> name;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> description;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> logoUrl;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> bannerUrl;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> address;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> city;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> province;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> country;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<String> postCode;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<BigDecimal> latitude;

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Optional<BigDecimal> longitude;
}
