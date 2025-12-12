package com.playvora.playvora_api.match.dtos;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.playvora.playvora_api.match.enums.MatchEventType;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMatchRequest {
    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<String> title = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<String> description = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<OffsetDateTime> matchDate = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<OffsetDateTime> registrationDeadline = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<String> bannerUrl = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<Integer> playersPerTeam = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<Boolean> isAutoSelection = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<Boolean> isPaidEvent = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<BigDecimal> pricePerPlayer = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<Boolean> isRefundable = Optional.empty();
                        
    @JsonSetter(nulls = Nulls.AS_EMPTY)              
        @Builder.Default
        private Optional<Integer> maxPlayers = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<BigDecimal> latitude = Optional.empty();
 
    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<BigDecimal> longitude = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<String> address = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<String> city = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<String> province = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<String> postCode = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<String> country = Optional.empty();

    @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private Optional<MatchEventType> type = Optional.empty();
}
