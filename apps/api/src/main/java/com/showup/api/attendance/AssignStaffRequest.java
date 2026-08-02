package com.showup.api.attendance;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record AssignStaffRequest(
        @NotNull UUID memberId,
        @NotNull StaffRole role,
        Instant shiftStartsAt,
        Instant shiftEndsAt,
        String notes) {
}
