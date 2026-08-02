package com.showup.api.attendance;

import com.showup.api.member.MemberSummary;

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
