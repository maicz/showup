package com.showup.api.rsvp;

import java.time.Instant;
import java.util.UUID;

public record RsvpSummary(
        UUID id,
        RsvpStatus status,
        int guestCount,
        Integer waitlistPosition,
        Instant respondedAt) {
}
