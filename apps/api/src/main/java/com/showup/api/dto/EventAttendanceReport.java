package com.showup.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A projection query result, not backed by an entity of its own. See
 * docs/domain-model.md#full-dto-catalog.
 */
public record EventAttendanceReport(
        UUID eventId,
        String eventTitle,
        int registeredCount,
        int attendedCount,
        int noShowCount,
        double attendanceRate,
        List<CheckInTime> checkInTimes,
        List<StaffScanTotal> staffScanTotals) {

    public record CheckInTime(Instant checkedInAt, int admittedCount) {
    }

    public record StaffScanTotal(UUID memberId, String displayName, int scanCount) {
    }
}
