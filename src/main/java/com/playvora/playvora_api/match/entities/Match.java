package com.playvora.playvora_api.match.entities;

import com.playvora.playvora_api.community.entities.Community;
import com.playvora.playvora_api.match.enums.MatchStatus;
import com.playvora.playvora_api.user.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name = "matches")
public class Match {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /**
     * User who created this match.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "banner_url")
    private String bannerUrl;

    
    @Column(name = "type")
    private String type;

    @Column(name = "match_date", nullable = false)
    private LocalDateTime matchDate;

    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;


    @Column(name = "players_per_team", nullable = false)
    private Integer playersPerTeam;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MatchStatus status = MatchStatus.UPCOMING;

    @Column(name = "is_auto_selection", nullable = false)
    @Builder.Default
    private boolean isAutoSelection = true;

    @Column(name = "draft_in_progress")
    @Builder.Default
    private Boolean draftInProgress = false;

    @JoinColumn(name = "current_picking_team_id", referencedColumnName = "id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Team currentPickingTeam;

    @Column(name = "manual_draft_order")
    private String manualDraftOrder;

    @Column(name = "manual_draft_index")
    @Builder.Default
    private Integer manualDraftIndex = 0;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "is_paid_event")
    @Builder.Default
    private Boolean isPaidEvent = false;


    @Column(name = "price_per_player")
    @Builder.Default
    private BigDecimal pricePerPlayer = BigDecimal.ZERO;

    @Column(name = "currency")
    @Builder.Default
    private String currency = "GBP";

    @Column(name = "is_refundable")
    @Builder.Default
    private Boolean isRefundable = true;

    @Column(name = "max_players")
    private Integer maxPlayers;

    @Column(name = "latitude")
    private BigDecimal latitude;

    @Column(name = "longitude")
    private BigDecimal longitude;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "province")
    private String province;

    @Column(name = "post_code")
    private String postCode;

    @Column(name = "country")
    private String country;

    @Column(name = "gender")
    private String gender;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Availability> availabilities;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Team> teams = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method for currentPickingTeamId
    public UUID getCurrentPickingTeamId() {
        return currentPickingTeam != null ? currentPickingTeam.getId() : null;
    }

    @Override
    public String toString() {
        return "Match{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", matchDate=" + matchDate +
                ", registrationDeadline=" + registrationDeadline +
                ", playersPerTeam=" + playersPerTeam +
                ", status=" + status +
                ", isAutoSelection=" + isAutoSelection +
                ", isPaidEvent=" + isPaidEvent +
                ", pricePerPlayer=" + pricePerPlayer +
                ", currency=" + currency +
                ", isRefundable=" + isRefundable +
                ", maxPlayers=" + maxPlayers +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", address=" + address +
                ", city=" + city +
                ", province=" + province +
                ", postCode=" + postCode +
                ", country=" + country +
                ", gender=" + gender +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
