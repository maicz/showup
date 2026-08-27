package com.showup.api.dto;

import com.showup.api.enums.CheckInMethod;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
        UUID id,
        String ticketCode,
        Instant checkedInAt,
        CheckInMethod method,
        int admittedCount,
        boolean alreadyCheckedIn,
        String attendeeName) {
}
