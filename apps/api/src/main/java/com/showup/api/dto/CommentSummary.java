package com.showup.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentSummary(
        UUID id,
        MemberSummary author,
        String body,
        UUID parentCommentId,
        Instant createdAt,
        Instant deletedAt) {
}
