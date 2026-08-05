package com.showup.api.dto;

import com.showup.api.enums.RsvpStatus;

import java.time.Instant;
import java.util.UUID;

public record RsvpSummary(
        UUID id,
        RsvpStatus status,
        int guestCount,
        Integer waitlistPosition,
        Instant respondedAt) {
}
