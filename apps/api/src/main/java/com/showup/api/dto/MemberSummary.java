package com.showup.api.dto;

import java.util.UUID;

public record MemberSummary(
        UUID id,
        String displayName,
        String photoUrl) {
}
