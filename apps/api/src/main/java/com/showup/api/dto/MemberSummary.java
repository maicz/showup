package com.showup.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MemberSummary(
        UUID id,
        String displayName,
        String photoUrl,
        String bio,
        String homeCity,
        Instant memberSince) {

    public MemberSummary(UUID id, String displayName, String photoUrl) {
        this(id, displayName, photoUrl, null, null, null);
    }
}
