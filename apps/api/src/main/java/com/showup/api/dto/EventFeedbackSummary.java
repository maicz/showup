package com.showup.api.dto;

import java.time.Instant;
import java.util.UUID;

public record EventFeedbackSummary(
        UUID id,
        MemberSummary member,
        int rating,
        String comment,
        Instant submittedAt) {
}
