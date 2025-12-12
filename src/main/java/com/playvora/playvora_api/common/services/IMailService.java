package com.playvora.playvora_api.common.services;

import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.venue.entities.VenueBooking;

import java.time.OffsetDateTime;

public interface IMailService {
    void sendPasswordResetEmail(User user, String resetLink, OffsetDateTime expiresAt);
    void sendVenueBookingConfirmation(User user, VenueBooking booking);
    void sendWaitlistConfirmationEmail(String email);
}

