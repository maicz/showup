package com.showup.api.dto;

import com.showup.api.enums.StaffRole;

import java.time.Instant;
import java.util.UUID;

public record StaffAssignmentSummary(
        UUID id,
        MemberSummary member,
        StaffRole role,
        Instant shiftStartsAt,
        Instant shiftEndsAt,
        String notes) {
}
