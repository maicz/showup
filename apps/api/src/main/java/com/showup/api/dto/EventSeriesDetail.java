package com.showup.api.dto;

import java.time.Instant;
import java.util.UUID;

public record EventSeriesDetail(
        UUID id,
        GroupSummary group,
        String recurrenceRule,
        Instant until,
        String templateTitle,
        String templateDescription,
        VenueSummary templateVenue,
        int templateDurationMinutes) {
}
