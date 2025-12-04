package com.playvora.playvora_api.match.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.playvora.playvora_api.community.dtos.CommunityResponse;
import com.playvora.playvora_api.match.enums.AvailabilityStatus;
import com.playvora.playvora_api.match.enums.MatchStatus;
import com.playvora.playvora_api.user.dtos.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MatchEventResponse {
    private UUID id;
    private CommunityResponse community;
    private String title;
    private String description;
    private LocalDateTime matchDate;
    private LocalDateTime registrationDeadline;
    private Integer playersPerTeam;
    private MatchStatus status;
    private boolean isAutoSelection;
    private Integer availablePlayers;
    private Integer totalPlayers;
    private LocalDateTime createdAt;
    private String currency;
    private LocalDateTime updatedAt;
    private List<TeamResponse> teams;
    private List<PlayerAvailablibityResponse> playersAvailability;
    private Boolean draftInProgress;
    private UUID currentPickingTeamId;
    private String currentPickingTeamName;
    private BigDecimal pricePerPlayer;
    private boolean isPaidEvent;
    private boolean isRefundable;
    private Integer maxPlayers;
    private UUID currentPickerId;
    private String currentPickerName;
    private List<UUID> draftOrder;
    private Integer draftIndex;
    private String type;
    private String address;
    private String city;
    private String province;
    private String postCode;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String bannerUrl;
    private String gender;
    public void setAvailablePlayers(Integer availablePlayers) {
        this.availablePlayers = availablePlayers;
    }

    public void setTotalPlayers(Integer totalPlayers) {
        this.totalPlayers = totalPlayers;
    }
}
