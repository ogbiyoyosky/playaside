package com.playvora.playvora_api.wallet.services;

import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.wallet.entities.Wallet;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public interface IWalletService {

    /**
     * Create a wallet for the given user if they don't already have one.
     * The wallet currency is derived from the user's country at creation time.
     */
    Wallet createWalletForUser(User user);

    /**
     * Get the wallet for a user, or null if it doesn't exist.
     */
    Wallet getWalletForUser(User user);

    /**
     * Increase the user's wallet balance by the given positive amount.
     */
    Wallet topUpWallet(User user, BigDecimal amount);
}


