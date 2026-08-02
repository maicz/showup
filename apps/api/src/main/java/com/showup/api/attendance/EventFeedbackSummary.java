package com.showup.api.attendance;

import com.showup.api.member.MemberSummary;

import java.time.Instant;
import java.util.UUID;

public record EventFeedbackSummary(
        UUID id,
        MemberSummary member,
        int rating,
        String comment,
        Instant submittedAt) {
}
