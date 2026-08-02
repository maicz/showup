package com.showup.api.reporting;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A projection query result, not backed by an entity of its own, covering a reporting period. */
public record GroupActivityReport(
        UUID groupId,
        String groupName,
        Instant periodStart,
        Instant periodEnd,
        int eventsHosted,
        int totalRsvps,
        int totalCheckIns,
        double averageAttendanceRate,
        BigDecimal averageRating) {
}
