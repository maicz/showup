package com.showup.api.dto;

import com.showup.api.enums.StaffRole;
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
