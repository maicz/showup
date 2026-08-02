package com.showup.api.attendance;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String code,
        int admitCount,
        Instant issuedAt,
        boolean revoked) {
}
