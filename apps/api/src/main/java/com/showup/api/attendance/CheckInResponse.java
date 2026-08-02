package com.showup.api.attendance;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
        UUID id,
        String ticketCode,
        Instant checkedInAt,
        CheckInMethod method,
        int admittedCount) {
}
