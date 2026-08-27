package com.showup.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyRsvpConfirmed(UUID memberId, UUID eventId, int seats) {
        log.info("Notification: RSVP confirmed for member {} at event {} (seats: {})", memberId, eventId, seats);
    }

    public void notifyWaitlistPromoted(UUID memberId, UUID eventId, int seats) {
        log.info("Notification: Member {} promoted from waitlist to seated for event {} (seats: {})", memberId, eventId, seats);
    }

    public void notifyEventCancelled(UUID eventId, String title, String reason) {
        log.info("Notification: Event '{}' ({}) was cancelled. Reason: {}", title, eventId, reason);
    }

    public void sendVerificationEmail(String email, String token) {
        log.info("Notification queued: email verification for {} (token fingerprint: {})",
                maskEmail(email), fingerprint(token));
    }

    public void sendPasswordResetEmail(String email, String token) {
        log.info("Notification queued: password reset for {} (token fingerprint: {})",
                maskEmail(email), fingerprint(token));
    }

    private static String maskEmail(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    /** Correlates delivery events without placing a usable bearer token in logs. */
    private static String fingerprint(String token) {
        return token == null ? "none" : Integer.toUnsignedString(token.hashCode(), 16);
    }
}
