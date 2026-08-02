package com.showup.api.group;

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
