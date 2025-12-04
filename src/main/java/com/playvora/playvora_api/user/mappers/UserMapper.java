package com.playvora.playvora_api.user.mappers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.playvora.playvora_api.user.dtos.UserResponse;
import com.playvora.playvora_api.user.dtos.UserRoleResponse;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.wallet.dto.WalletResponse;
import com.playvora.playvora_api.wallet.mappers.WalletMapper;
import org.hibernate.Hibernate;

public class UserMapper {
    public static UserResponse convertToResponse(User user) {

        // Safely map roles only if the collection is initialized to avoid lazy loading
        List<UserRoleResponse> userRoles;
        if (Hibernate.isInitialized(user.getUserRoles())) {
            userRoles = user.getUserRoles().stream()
                    .map(UserRoleMapper::convertToResponse)
                    .collect(Collectors.toList());
        } else {
            userRoles = Collections.emptyList();
        }

        // Safely map wallet only if it is initialized to avoid lazy loading
        String walletCurrency = null;
        WalletResponse walletResponse = null;
        if (user.getWallet() != null && Hibernate.isInitialized(user.getWallet())) {
            walletCurrency = user.getWallet().getCurrency();
            walletResponse = WalletMapper.convertToResponse(user.getWallet());
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .nickname(user.getNickname())
                .profilePictureUrl(user.getProfilePictureUrl())
                .enabled(user.isEnabled())
                .userRoles(userRoles)
                .country(user.getCountry())
                .walletCurrency(walletCurrency)
                .wallet(walletResponse)
                // By default, expose an empty map so clients never see `registeredEvents: null`.
                // Endpoints that have specific match/event context can override this field.
                .registeredCommunities(Collections.emptyMap())
                .registeredEvents(Collections.emptyMap())
                .build();
    }
}
