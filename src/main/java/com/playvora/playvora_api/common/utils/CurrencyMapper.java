package com.playvora.playvora_api.common.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class CurrencyMapper {

    // Eurozone country list
    private static final Set<String> EURO_COUNTRIES = new HashSet<>(Arrays.asList(
        "austria", "belgium", "croatia", "cyprus", "estonia", "finland",
        "france", "germany", "greece", "ireland", "italy", "latvia",
        "lithuania", "luxembourg", "malta", "netherlands", "portugal",
        "slovakia", "slovenia", "spain"
    ));


    private CurrencyMapper() {
        // Prevent instantiation
    }
    /**
     * Returns the currency code for a given country.
     *
     * @param countryName country name as String
     * @return "EUR" for Eurozone countries, "GBP" for UK, otherwise "USD"
     */
    public static String getCurrency(String countryName) {
        if (countryName == null || countryName.isEmpty()) {
            return "USD";
        }

        String normalized = countryName.trim().toLowerCase(Locale.ROOT);

        if (normalized.equals("united kingdom") 
                || normalized.equals("uk") 
                || normalized.equals("great britain")
                || normalized.equals("england")
                || normalized.equals("scotland")
                || normalized.equals("wales")
                || normalized.equals("northern ireland")) {
            return "GBP";
        }

        if (EURO_COUNTRIES.contains(normalized)) {
            return "EUR";
        }

        return "USD";
    }
}
