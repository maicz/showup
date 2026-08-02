package com.showup.api.rsvp;

import com.showup.api.member.MemberSummary;

import java.time.Instant;

public record AttendeeSummary(
        MemberSummary member,
        RsvpStatus status,
        int guestCount,
        Instant respondedAt) {
}
