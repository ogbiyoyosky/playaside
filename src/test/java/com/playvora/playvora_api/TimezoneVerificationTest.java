package com.playvora.playvora_api;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class TimezoneVerificationTest {

    @Test
    public void testOffsetDateTimeMaintainsTimezone() {
        // Test that OffsetDateTime objects maintain timezone information
        OffsetDateTime utcTime = OffsetDateTime.of(2025, 12, 10, 15, 30, 45, 123456789, ZoneOffset.UTC);
        OffsetDateTime nyTime = OffsetDateTime.of(2025, 12, 10, 10, 30, 45, 123456789, ZoneOffset.of("-05:00"));

        System.out.println("UTC time: " + utcTime);
        System.out.println("UTC offset: " + utcTime.getOffset());
        System.out.println("NY time: " + nyTime);
        System.out.println("NY offset: " + nyTime.getOffset());

        // Verify timezone information is preserved
        assertThat(utcTime.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(nyTime.getOffset()).isEqualTo(ZoneOffset.of("-05:00"));

        // Test that PostgreSQL TIMESTAMPTZ columns should store this correctly
        // when Java OffsetDateTime objects are saved
        assertThat(utcTime.toString()).matches(".+(Z|[+-]\\d{2}(:\\d{2})?)$");
        assertThat(nyTime.toString()).contains("-05:00");
    }

    @Test
    public void testCurrentTimeHasTimezone() {
        // Test that OffsetDateTime.now() includes timezone information
        OffsetDateTime now = OffsetDateTime.now();

        System.out.println("Current time with timezone: " + now);
        System.out.println("Timezone offset: " + now.getOffset());

        // Verify it has timezone information
        assertThat(now.getOffset()).isNotNull();

        // When saved to PostgreSQL TIMESTAMPTZ, this should preserve the timezone
        String isoString = now.toString();
        System.out.println("ISO string (what gets sent to DB): " + isoString);

        // Should contain timezone offset like +00, +05:30, -08:00, or Z for UTC
        assertThat(isoString).matches(".+([+-]\\d{2}(:\\d{2})?|Z)$");
    }
}