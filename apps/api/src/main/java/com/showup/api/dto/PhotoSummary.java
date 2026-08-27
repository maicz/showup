package com.showup.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PhotoSummary(
        UUID id,
        MemberSummary uploadedBy,
        String url,
        String caption,
        int width,
        int height,
        Instant createdAt) {
}
