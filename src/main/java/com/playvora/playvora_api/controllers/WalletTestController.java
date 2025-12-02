package com.playvora.playvora_api.controllers;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Hidden // Hide from Swagger documentation
public class WalletTestController {

    /**
     * Serves the wallet top-up test page.
     * Access at: /wallet-topup-test
     */
    @GetMapping("/wallet-topup-test")
    public String walletTopupTest() {
        return "wallet-topup-test";
    }

    /**
     * Serves the Stripe Checkout wallet test page.
     * Access at: /wallet-checkout-test
     */
    @GetMapping("/wallet-checkout-test")
    public String walletCheckoutTest() {
        return "wallet-checkout-test";
    }
}


