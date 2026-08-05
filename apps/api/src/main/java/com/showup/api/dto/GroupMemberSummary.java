package com.showup.api.dto;

import com.showup.api.enums.GroupMemberRole;
import com.showup.api.enums.GroupMembershipStatus;

import java.time.Instant;
import java.util.UUID;

public record GroupMemberSummary(
        UUID memberId,
        String displayName,
        String photoUrl,
        GroupMemberRole role,
        GroupMembershipStatus status,
        Instant joinedAt) {
}
