package com.playvora.playvora_api.match.dtos;

import com.playvora.playvora_api.match.enums.Gender;
import com.playvora.playvora_api.match.enums.MatchEventType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateMatchRequest {
    @NotNull(message = "Community ID is required")
    private UUID communityId;

    @NotBlank(message = "Match title is required")
    @Size(max = 255, message = "Match title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Match date is required")
    @Future(message = "Match date must be in the future")
    private LocalDateTime matchDate;
    
    @NotBlank(message = "Banner URL is required")
    @Size(max = 255, message = "Banner URL must not exceed 255 characters")
    private String bannerUrl;

    @NotNull(message = "Registration deadline is required")
    @Future(message = "Registration deadline must be in the future")
    private LocalDateTime registrationDeadline;

    @NotNull(message = "Players per team is required")
    @Min(value = 1, message = "Players per team must be at least 1")
    private Integer playersPerTeam;

    @NotNull(message = "Auto selection preference is required")
    private Boolean isAutoSelection;

    @NotNull(message = "Is paid event is required")
    private Boolean isPaidEvent;

    @NotNull(message = "Price per player is required")
    private BigDecimal pricePerPlayer;

    @NotNull(message = "Is refundable is required")
    private Boolean isRefundable;

    @NotNull(message = "Max players is required")
    @Min(value = 1, message = "Max players must be at least 1")
    private Integer maxPlayers;

    @NotNull(message = "Latitude is required")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    private BigDecimal longitude;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Size(max = 255, message = "City must not exceed 255 characters")
    private String city;

    @Size(max = 255, message = "Province must not exceed 255 characters")
    private String province;

    @Size(max = 255, message = "Post code must not exceed 255 characters")
    private String postCode;

    @Size(max = 255, message = "Country must not exceed 255 characters")
    private String country;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Match type is required")
    @Enumerated(EnumType.STRING)
    private MatchEventType type;
}
