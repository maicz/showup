package com.showup.api.dto;

import java.util.UUID;

public record CategorySummary(
        UUID id,
        String slug,
        String name,
        String iconUrl,
        int displayOrder) {
}
