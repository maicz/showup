package com.showup.api.topic;

import java.util.UUID;

public record CategorySummary(
        UUID id,
        String slug,
        String name,
        String iconUrl,
        int displayOrder) {
}
