package com.playvora.playvora_api.wallet.mappers;

import com.playvora.playvora_api.wallet.dto.WalletResponse;
import com.playvora.playvora_api.wallet.entities.Wallet;

public class WalletMapper {
    public static WalletResponse convertToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }
    
}
