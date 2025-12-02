package com.playvora.playvora_api.common.services.impl;

import com.playvora.playvora_api.common.exception.BadRequestException;
import com.playvora.playvora_api.common.services.IMailService;
import com.playvora.playvora_api.user.entities.User;
import com.playvora.playvora_api.venue.entities.VenueBooking;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService implements IMailService {
    private static final DateTimeFormatter HUMAN_READABLE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d yyyy 'at' HH:mm z");

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${application.mail.from:no-reply@playaside.com}")
    private String fromAddress;

    @Override
    public void sendPasswordResetEmail(User user, String resetLink, LocalDateTime expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setFrom(fromAddress);
        message.setSubject("Playaside Password Reset Instructions");

        String formattedExpiry = expiresAt.atZone(java.time.ZoneId.systemDefault())
                .format(HUMAN_READABLE_FORMAT);

        message.setText(buildMessageBody(user, resetLink, formattedExpiry));

        try {
            mailSender.send(message);
        } catch (Exception exception) {
            log.error("Failed to send password reset email to {}", user.getEmail(), exception);
            throw new BadRequestException("Unable to send password reset email. Please try again later.");
        }
    }

    @Override
    public void sendWaitlistConfirmationEmail(String email) {
        try {
            jakarta.mail.internet.MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setTo(email);
            helper.setFrom(fromAddress);
            helper.setSubject("Welcome to the Playaside waitlist");

            Map<String, Object> variables = new HashMap<>();
            variables.put("email", email);

            Context context = new Context();
            context.setVariables(variables);

            String htmlBody = templateEngine.process("waitlist-email", context);
            helper.setText(htmlBody, true);

            mailSender.send(mimeMessage);
        } catch (Exception exception) {
            log.error("Failed to send waitlist confirmation email to {}", email, exception);
            throw new BadRequestException("Unable to send waitlist confirmation email. Please try again later.");
        }
    }

    @Override
    public void sendVenueBookingConfirmation(User user, VenueBooking booking) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setFrom(fromAddress);
        message.setSubject("Your Playaside venue booking is confirmed");

        String start = booking.getStartTime().atZone(java.time.ZoneId.systemDefault())
                .format(HUMAN_READABLE_FORMAT);
        String end = booking.getEndTime().atZone(java.time.ZoneId.systemDefault())
                .format(HUMAN_READABLE_FORMAT);

        String body = String.format("""
                Hi %s,

                Your booking for venue "%s" has been confirmed.

                Booking details:
                - Start: %s
                - End: %s
                - Total price: %s

                Thank you for using Playaside!

                The Playvora Team
                """,
                user.getFirstName() != null ? user.getFirstName() : "there",
                booking.getVenue().getName(),
                start,
                end,
                booking.getTotalPrice().toPlainString()
        );

        message.setText(body);

        try {
            mailSender.send(message);
        } catch (Exception exception) {
            log.error("Failed to send venue booking confirmation email to {}", user.getEmail(), exception);
            // Do not throw here to avoid failing booking confirmation due to email issues
        }
    }

    private String buildMessageBody(User user, String resetLink, String formattedExpiry) {
        return String.format("""
                Hi %s,

                We received a request to reset your Playaside password. You can set a new password by visiting the link below:

                %s

                This link will expire on %s. If you did not request a password reset, you can safely ignore this email.

                Thanks,
                The Playaside Team
                """, user.getFirstName() != null ? user.getFirstName() : "there", resetLink, formattedExpiry);
    }
}

