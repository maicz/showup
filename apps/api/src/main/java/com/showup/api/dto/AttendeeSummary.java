package com.showup.api.dto;

import com.showup.api.enums.RsvpStatus;

import java.time.Instant;

public record AttendeeSummary(
        MemberSummary member,
        RsvpStatus status,
        int guestCount,
        Instant respondedAt) {
}
