package com.playvora.playvora_api.user.dtos;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.playvora.playvora_api.wallet.dto.WalletResponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String nickname;
    private boolean enabled;
    private String profilePictureUrl;
    private String country;
    private WalletResponse wallet;

    /**
     * Currency of the user's wallet, derived from their country when the wallet
     * is first created. This value is immutable once set.
     */
    private String walletCurrency;

    private List<UserRoleResponse> userRoles;

    /**
     * Map of event/match ID -> whether the user is registered for that event.
     * This is typically populated per-context (e.g. for a specific list of matches).
     */
    private Map<UUID, Boolean> registeredEvents;
    private Map<UUID, Boolean> registeredCommunities;
}
