package com.showup.api.event;

import com.showup.api.group.GroupSummary;
import com.showup.api.venue.VenueSummary;

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
