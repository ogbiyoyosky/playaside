package com.playvora.playvora_api.user.mappers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.playvora.playvora_api.user.dtos.UserResponse;
import com.playvora.playvora_api.user.dtos.UserRoleResponse;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.wallet.mappers.WalletMapper;

public class UserMapper {
    public static UserResponse convertToResponse(User user) {

        List<UserRoleResponse> userRoles = user.getUserRoles().stream()
                .map(UserRoleMapper::convertToResponse)
                .collect(Collectors.toList());

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
                .walletCurrency((user.getWallet() != null) ? user.getWallet().getCurrency() : null)
                .wallet(user.getWallet() != null ? WalletMapper.convertToResponse(user.getWallet()) : null)
                // By default, expose an empty map so clients never see `registeredEvents: null`.
                // Endpoints that have specific match/event context can override this field.
                .registeredEvents(Collections.emptyMap())
                .build();
    }
}
