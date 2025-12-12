package com.playvora.playvora_api.wallet.services.impl;

import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.utils.CurrencyMapper;
import com.playvora.playvora_api.payment.services.ITransactionService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.wallet.entities.Wallet;
import com.playvora.playvora_api.wallet.repo.WalletRepository;
import com.playvora.playvora_api.wallet.services.IWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService implements IWalletService {

    private final WalletRepository walletRepository;
    private final ITransactionService transactionService;

    @Override
    @Transactional
    public Wallet createWalletForUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("Cannot create wallet for unknown user");
        }

        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    String country = user.getCountry();
                    if (country == null || country.isBlank()) {
                        throw new BadRequestException("Country must be set before creating a wallet");
                    }

                    String currency = CurrencyMapper.getCurrency(country);
                    log.info("Creating wallet for user {} with currency {} (country={})",
                            user.getId(), currency, country);

                    Wallet wallet = Wallet.builder()
                            .user(user)
                            .currency(currency)
                            .countrySnapshot(country)
                            .build();

                    return walletRepository.save(wallet);
                });
    }

    @Override
    public Wallet getWalletForUser(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        return walletRepository.findByUserId(user.getId()).orElse(null);
    }

    @Override
    @Transactional
    public Wallet topUpWallet(User user, BigDecimal amount) {
        if (user == null || user.getId() == null) {
            throw new BadRequestException("Cannot top up wallet for unknown user");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Top up amount must be positive");
        }

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Wallet not found for user"));

        wallet.setBalance(wallet.getBalance().add(amount));
        log.info("Topped up wallet {} for user {} by {}. New balance: {}",
                wallet.getId(), user.getId(), amount, wallet.getBalance());

        Wallet saved = walletRepository.save(wallet);

        // Record transaction for this top-up
        transactionService.recordWalletTopup(
                user,
                saved,
                amount,
                saved.getCurrency(),
                "Wallet top-up",
                null
        );

        return saved;
    }
}


